package dev.detpikachu.unpluggedafk.network;

import dev.detpikachu.unpluggedafk.KickReasons;
import dev.detpikachu.unpluggedafk.common.network.Protocol;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageDecoder;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageEncoder;
import dev.detpikachu.unpluggedafk.common.network.messages.Goodbye;
import dev.detpikachu.unpluggedafk.common.network.messages.Heartbeat;
import dev.detpikachu.unpluggedafk.common.network.messages.Relay;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionAck;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionEnd;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionStart;
import dev.detpikachu.unpluggedafk.common.network.messages.Sync;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.ScheduledFuture;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class LinkClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int HANDSHAKE_TIMEOUT_SECS = 10;
    private static final int WORKER_THREADS = 1;
    private static final int BACKOFF_MIN_SECS = 1;
    private static final int BACKOFF_MAX_SECS = 5;
    private static final int SHUTDOWN_WAIT_SECS = 1;
    private static final int ACK_TIMEOUT_SECS = 5;
    private static final String TEXTURES_PROPERTY = "textures";
    private static final String END_TIMED_OUT = "ACK_TIMEOUT";

    private final ConcurrentHashMap<UUID, PendingSession> pendingSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, EndedSession> endedSessions = new ConcurrentHashMap<>();

    private volatile boolean running;
    private volatile @Nullable Channel channel;
    private volatile @Nullable ScheduledFuture<?> heartbeat;
    private volatile @Nullable EventLoopGroup workers;
    private volatile @Nullable Bootstrap bootstrap;

    private boolean quiet;
    private int backoffSecs;

    public void start() {
        if (this.running) {
            return;
        }

        final var options = Options.getInstance().getLink();
        final var group = new MultiThreadIoEventLoopGroup(WORKER_THREADS, NioIoHandler.newFactory());

        this.quiet = false;
        this.backoffSecs = BACKOFF_MIN_SECS;
        this.workers = group;
        this.bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .remoteAddress(options.getHost(), options.getPort())
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socket) {
                        socket.pipeline()
                                .addLast("timeout", new ReadTimeoutHandler(HANDSHAKE_TIMEOUT_SECS))
                                .addLast(
                                        "splitter",
                                        new LengthFieldBasedFrameDecoder(
                                                Protocol.MAX_FRAME_BYTES,
                                                0,
                                                Protocol.LENGTH_FIELD_BYTES,
                                                0,
                                                Protocol.LENGTH_FIELD_BYTES))
                                .addLast("decoder", new MessageDecoder())
                                .addLast("prepender", new LengthFieldPrepender(Protocol.LENGTH_FIELD_BYTES))
                                .addLast("encoder", new MessageEncoder())
                                .addLast("handler", new LinkHandler(LinkClient.this, options));
                    }
                });
        this.running = true;

        connect();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void stop() {
        if (!this.running) {
            return;
        }
        this.running = false;

        cancelHeartbeat();

        try {
            failPending(ChatMessages.REFUSED_UNREACHABLE);
        } finally {
            final var channel = this.channel;
            if (channel != null && channel.isActive()) {
                final var future = channel.writeAndFlush(new Goodbye(KickReasons.DISABLED));

                try {
                    future.await(SHUTDOWN_WAIT_SECS, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }

            final var group = this.workers;
            if (group != null) {
                group.shutdownGracefully(0, SHUTDOWN_WAIT_SECS, TimeUnit.SECONDS);
            }

            this.channel = null;
            this.workers = null;
            this.bootstrap = null;
        }
    }

    public boolean isReady() {
        final var channel = this.channel;
        return channel != null && channel.isActive();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void startSession(ServerPlayer player, long secondsRemaining, Consumer<SessionAck> onAck) {
        final var uuid = player.getUUID();
        final var channel = this.channel;

        if (channel == null || !channel.isActive()) {
            logDebug("Refusing SESSION_START for {} ({}). The link is down.", player.getPlainTextName(), uuid);
            onAck.accept(new SessionAck(uuid, false, ChatMessages.REFUSED_UNREACHABLE));
            return;
        }

        if (this.pendingSessions.containsKey(uuid)) {
            LOGGER.warn(
                    "Refusing a second SESSION_START for {} ({}). One is already pending.",
                    player.getPlainTextName(),
                    uuid);
            onAck.accept(new SessionAck(uuid, false, ChatMessages.REFUSED_IN_FLIGHT));
            return;
        }

        logDebug(
                "Sending SESSION_START for {} ({}), {} second(s) remaining.",
                player.getPlainTextName(),
                uuid,
                secondsRemaining);

        final var timeout = channel.eventLoop().schedule(() -> timedOut(uuid), ACK_TIMEOUT_SECS, TimeUnit.SECONDS);
        this.pendingSessions.put(uuid, new PendingSession(onAck, timeout));

        channel.writeAndFlush(describe(player, secondsRemaining));
    }

    public void endSession(ServerPlayer bot, String reason) {
        if (!(bot instanceof UnpluggedServerPlayer unplugged)
                || !unplugged.getSession().isFake()) {
            this.endedSessions.put(bot.getUUID(), EndedSession.of(describe(bot, 0L)));
        }

        pruneEndedSessions();
        this.endSession(bot.getUUID(), reason);
    }

    public void endSession(UUID uuid, String reason) {
        final var pending = this.pendingSessions.remove(uuid);

        if (pending != null) {
            pending.timeout().cancel(false);
        }

        sendEnd(uuid, reason);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void sendEnd(UUID uuid, String reason) {
        final var channel = this.channel;

        if (channel == null || !channel.isActive()) {
            logDebug("Could not send SESSION_END for {}: {}. The link is down.", uuid, reason);
            return;
        }

        logDebug("Sending SESSION_END for {}: {}.", uuid, reason);
        channel.writeAndFlush(new SessionEnd(uuid, reason));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void relay(UUID uuid, String channelName, byte[] payload) {
        final var channel = this.channel;

        if (channel == null || !channel.isActive()) {
            logDebug("Dropped a plugin message on {} for bot {}. The link is down.", channelName, uuid);
            return;
        }

        if (payload.length > Protocol.MAX_PAYLOAD_BYTES) {
            LOGGER.warn(
                    "Dropped a {} byte(s) plugin message on {} for bot {}. The link carries at most {}.",
                    payload.length,
                    channelName,
                    uuid,
                    Protocol.MAX_PAYLOAD_BYTES);
            return;
        }

        logDebug("Relaying {} byte(s) from bot {} to the proxy on channel {}.", payload.length, uuid, channelName);
        channel.writeAndFlush(new Relay(uuid, channelName, payload));
    }

    void acknowledged(SessionAck ack) {
        final var pending = this.pendingSessions.remove(ack.uuid());

        if (pending == null) {
            logDebug("Ignoring a SESSION_ACK for {}. Nothing was waiting on it.", ack.uuid());
            return;
        }

        logDebug("SESSION_ACK for {}: accepted={} reason={}", ack.uuid(), ack.accepted(), ack.reason());
        pending.timeout().cancel(false);
        pending.callback().accept(ack);
    }

    void established(Channel channel, String serverName) {
        this.channel = channel;
        this.quiet = false;
        this.backoffSecs = BACKOFF_MIN_SECS;
        this.heartbeat = channel.eventLoop()
                .scheduleAtFixedRate(
                        () -> beat(channel), Protocol.HEARTBEAT_SECS, Protocol.HEARTBEAT_SECS, TimeUnit.SECONDS);

        LOGGER.info("Linked to the proxy at {} as {}.", channel.remoteAddress(), serverName);
        sync(channel);
    }

    void disconnected(boolean wasReady) {
        this.channel = null;
        cancelHeartbeat();
        logDebug("Link closed. Ready before the close: {}.", wasReady);
        failPending(ChatMessages.REFUSED_UNREACHABLE);

        if (wasReady) {
            warnOnce("Link to the proxy lost. Reconnecting in the background.");
        }

        scheduleReconnect();
    }

    void warnOnce(String message, Object... arguments) {
        if (this.quiet) {
            return;
        }

        this.quiet = true;
        LOGGER.warn(message, arguments);
    }

    void errorOnce(String message, Object... arguments) {
        if (this.quiet) {
            return;
        }

        this.quiet = true;
        LOGGER.error(message, arguments);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void sync(Channel channel) {
        final var sessions = new ArrayList<SessionStart>();

        for (final var bot : SessionRegistry.getInstance().all()) {
            if (bot.getSession().isFake()) {
                continue;
            }

            sessions.add(describe(bot, bot.getSession().remaining().toSeconds()));
        }

        pruneEndedSessions();
        this.endedSessions
                .values()
                .forEach(ended -> sessions.add(
                        new SessionStart(ended.uuid(), ended.username(), ended.skin(), -ended.secondsSinceEnd())));

        logDebug("Sending SYNC with {} session(s).", sessions.size());
        channel.writeAndFlush(new Sync(sessions));
    }

    private void timedOut(UUID uuid) {
        final var pending = this.pendingSessions.remove(uuid);

        if (pending == null) {
            return;
        }

        LOGGER.warn("The proxy did not acknowledge the unplug of {} in time. Undoing the session.", uuid);
        sendEnd(uuid, END_TIMED_OUT);
        answer(pending, uuid, ChatMessages.REFUSED_TIMED_OUT);
    }

    private void failPending(String reason) {
        this.pendingSessions.keySet().forEach(uuid -> {
            final var pending = this.pendingSessions.remove(uuid);

            if (pending != null) {
                pending.timeout().cancel(false);
                answer(pending, uuid, reason);
            }
        });
    }

    private static void answer(PendingSession pending, UUID uuid, String reason) {
        try {
            pending.callback().accept(new SessionAck(uuid, false, reason));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to answer the pending unplug of {}.", uuid, exception);
        }
    }

    private void pruneEndedSessions() {
        this.endedSessions.values().removeIf(ended -> ended.secondsSinceEnd() > Protocol.GRACE_SECS);
    }

    private void cancelHeartbeat() {
        final var heartbeat = this.heartbeat;

        if (heartbeat != null) {
            heartbeat.cancel(false);
            this.heartbeat = null;
        }
    }

    private void connect() {
        final var bootstrap = this.bootstrap;

        if (bootstrap == null || !this.running) {
            return;
        }

        bootstrap.connect().addListener(attempt -> {
            if (attempt.isSuccess()) {
                return;
            }

            warnOnce("Cannot reach the proxy link. Retrying in the background.", attempt.cause());
            scheduleReconnect();
        });
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void scheduleReconnect() {
        final var group = this.workers;

        if (group == null || !this.running) {
            return;
        }

        final var delay = this.backoffSecs;
        this.backoffSecs = Math.min(this.backoffSecs * 2, BACKOFF_MAX_SECS);
        group.schedule(this::connect, delay, TimeUnit.SECONDS);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void beat(Channel channel) {
        if (!channel.isActive()) {
            return;
        }

        channel.writeAndFlush(new Heartbeat(System.nanoTime()));
    }

    private static SessionStart describe(ServerPlayer player, long secondsRemaining) {
        return new SessionStart(player.getUUID(), player.getPlainTextName(), skinOf(player), secondsRemaining);
    }

    private static SessionStart.@Nullable Skin skinOf(ServerPlayer player) {
        final var textures =
                player.getGameProfile().properties().get(TEXTURES_PROPERTY).iterator();

        if (!textures.hasNext()) {
            return null;
        }

        final var property = textures.next();
        return new SessionStart.Skin(property.value(), property.signature());
    }

    private record PendingSession(Consumer<SessionAck> callback, ScheduledFuture<?> timeout) {}

    private record EndedSession(UUID uuid, String username, SessionStart.@Nullable Skin skin, Instant endedAt) {

        static EndedSession of(SessionStart start) {
            return new EndedSession(start.uuid(), start.username(), start.skin(), Instant.now());
        }

        long secondsSinceEnd() {
            return Duration.between(this.endedAt, Instant.now()).toSeconds();
        }
    }
}
