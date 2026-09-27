package dev.detpikachu.unpluggedafk.network;

import dev.detpikachu.unpluggedafk.KickReasons;
import dev.detpikachu.unpluggedafk.common.logging.Log;
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
import dev.detpikachu.unpluggedafk.format.ChatMessages;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.Session;
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
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
    private final ConcurrentHashMap<UUID, CommittedSession> committedSessions = new ConcurrentHashMap<>();
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
    public void startSession(ServerPlayer player, Session session, Consumer<SessionAck> onAck) {
        final var uuid = player.getUUID();
        final var channel = this.channel;

        if (channel == null || !channel.isActive()) {
            Log.debug("Refusing SESSION_START for {} ({}). The link is down.", player.getPlainTextName(), uuid);
            onAck.accept(new SessionAck(uuid, false, ChatMessages.REFUSED_UNREACHABLE));
            return;
        }

        if (this.pendingSessions.containsKey(uuid)) {
            Log.warn(
                    "Refusing a second SESSION_START for {} ({}). One is already pending.",
                    player.getPlainTextName(),
                    uuid);
            onAck.accept(new SessionAck(uuid, false, ChatMessages.REFUSED_IN_FLIGHT));
            return;
        }

        final var secondsRemaining = session.remaining().toSeconds();

        Log.debug(
                "Sending SESSION_START for {} ({}), {} second(s) remaining.",
                player.getPlainTextName(),
                uuid,
                secondsRemaining);

        final var start = describe(player, session, secondsRemaining);
        final var timeout = channel.eventLoop().schedule(() -> timedOut(uuid), ACK_TIMEOUT_SECS, TimeUnit.SECONDS);

        this.pendingSessions.put(uuid, new PendingSession(onAck, timeout));
        pruneCommittedSessions();
        this.committedSessions.put(uuid, new CommittedSession(start, session));

        channel.writeAndFlush(start);
    }

    public void endSession(UnpluggedServerPlayer bot, String reason) {
        final var session = bot.getSession();

        if (!session.isFake()) {
            this.endedSessions.put(bot.getUUID(), EndedSession.of(describe(bot, session, 0L)));
        }

        pruneEndedSessions();
        this.endSession(bot.getUUID(), reason);
    }

    public void endSession(UUID uuid, String reason) {
        final var pending = this.pendingSessions.remove(uuid);

        if (pending != null) {
            pending.timeout().cancel(false);
        }

        this.committedSessions.remove(uuid);
        sendEnd(uuid, reason);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void sendEnd(UUID uuid, String reason) {
        final var channel = this.channel;

        if (channel == null || !channel.isActive()) {
            Log.debug("Could not send SESSION_END for {}: {}. The link is down.", uuid, reason);
            return;
        }

        Log.debug("Sending SESSION_END for {}: {}.", uuid, reason);
        channel.writeAndFlush(new SessionEnd(uuid, reason));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void relay(UUID uuid, String channelName, byte[] payload) {
        final var channel = this.channel;

        if (channel == null || !channel.isActive()) {
            Log.debug("Dropped a plugin message on {} for bot {}. The link is down.", channelName, uuid);
            return;
        }

        if (payload.length > Protocol.MAX_PAYLOAD_BYTES) {
            Log.warn(
                    "Dropped a {} byte(s) plugin message on {} for bot {}. The link carries at most {}.",
                    payload.length,
                    channelName,
                    uuid,
                    Protocol.MAX_PAYLOAD_BYTES);
            return;
        }

        Log.debug("Relaying {} byte(s) from bot {} to the proxy on channel {}.", payload.length, uuid, channelName);
        channel.writeAndFlush(new Relay(uuid, channelName, payload));
    }

    void acknowledged(SessionAck ack) {
        final var pending = this.pendingSessions.remove(ack.uuid());

        if (pending == null) {
            Log.debug("Ignoring a SESSION_ACK for {}. Nothing was waiting on it.", ack.uuid());
            return;
        }

        Log.debug("SESSION_ACK for {}: accepted={} reason={}", ack.uuid(), ack.accepted(), ack.reason());
        pending.timeout().cancel(false);

        if (!ack.accepted()) {
            this.committedSessions.remove(ack.uuid());
        }

        pending.callback().accept(ack);
    }

    void established(Channel channel, String serverName) {
        this.channel = channel;
        this.quiet = false;
        this.backoffSecs = BACKOFF_MIN_SECS;
        this.heartbeat = channel.eventLoop()
                .scheduleAtFixedRate(
                        () -> beat(channel), Protocol.HEARTBEAT_SECS, Protocol.HEARTBEAT_SECS, TimeUnit.SECONDS);

        Log.info("Linked to the proxy at {} as {}.", channel.remoteAddress(), serverName);
        sync(channel);
    }

    void disconnected(boolean wasReady) {
        this.channel = null;
        cancelHeartbeat();
        Log.debug("Link closed. Ready before the close: {}.", wasReady);
        failPending(ChatMessages.REFUSED_UNREACHABLE);

        if (wasReady && this.running) {
            warnOnce("Link to the proxy lost. Reconnecting in the background.");
        }

        scheduleReconnect();
    }

    void warnOnce(String message, Object... arguments) {
        if (this.quiet) {
            return;
        }

        this.quiet = true;
        Log.warn(message, arguments);
    }

    void errorOnce(String message, Object... arguments) {
        if (this.quiet) {
            return;
        }

        this.quiet = true;
        Log.error(message, arguments);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void sync(Channel channel) {
        final var sessions = new ArrayList<SessionStart>();
        final var seen = new HashSet<UUID>();

        for (final var bot : SessionRegistry.getInstance().all()) {
            final var session = bot.getSession();

            if (session.isFake()) {
                continue;
            }

            seen.add(bot.getUUID());
            sessions.add(describe(bot, session, session.remaining().toSeconds()));
        }

        pruneCommittedSessions();
        this.committedSessions.forEach((uuid, committed) -> {
            if (seen.add(uuid)) {
                sessions.add(committed.current());
            }
        });

        pruneEndedSessions();
        this.endedSessions.forEach((uuid, ended) -> {
            if (seen.add(uuid)) {
                sessions.add(ended.hint());
            }
        });

        Log.debug("Sending SYNC with {} session(s).", sessions.size());
        channel.writeAndFlush(new Sync(sessions));
    }

    private void timedOut(UUID uuid) {
        final var pending = this.pendingSessions.remove(uuid);

        if (pending == null) {
            return;
        }

        Log.warn("The proxy did not acknowledge the unplug of {} in time. Undoing the session.", uuid);
        this.committedSessions.remove(uuid);
        sendEnd(uuid, END_TIMED_OUT);
        answer(pending, uuid, ChatMessages.REFUSED_TIMED_OUT);
    }

    private void failPending(String reason) {
        this.pendingSessions.keySet().forEach(uuid -> {
            final var pending = this.pendingSessions.remove(uuid);

            if (pending != null) {
                pending.timeout().cancel(false);
                this.committedSessions.remove(uuid);
                answer(pending, uuid, reason);
            }
        });
    }

    private static void answer(PendingSession pending, UUID uuid, String reason) {
        try {
            pending.callback().accept(new SessionAck(uuid, false, reason));
        } catch (RuntimeException exception) {
            Log.error("Failed to answer the pending unplug of {}.", uuid, exception);
        }
    }

    private void pruneCommittedSessions() {
        final var registry = SessionRegistry.getInstance();

        this.committedSessions.keySet().removeIf(uuid -> !registry.isUnplugging(uuid) && registry.find(uuid) == null);
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

    private static SessionStart describe(ServerPlayer player, Session session, long secondsRemaining) {
        return new SessionStart(
                player.getUUID(),
                player.getPlainTextName(),
                skinOf(player),
                session.durationMins(),
                session.reason(),
                secondsRemaining);
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

    private record CommittedSession(SessionStart start, Session session) {

        SessionStart current() {
            return new SessionStart(
                    this.start.uuid(),
                    this.start.username(),
                    this.start.skin(),
                    this.start.durationMins(),
                    this.start.reason(),
                    this.session.remaining().toSeconds());
        }
    }

    private record EndedSession(SessionStart start, Instant endedAt) {

        static EndedSession of(SessionStart start) {
            return new EndedSession(start, Instant.now());
        }

        SessionStart hint() {
            return new SessionStart(
                    this.start.uuid(),
                    this.start.username(),
                    this.start.skin(),
                    this.start.durationMins(),
                    this.start.reason(),
                    -this.secondsSinceEnd());
        }

        long secondsSinceEnd() {
            return Duration.between(this.endedAt, Instant.now()).toSeconds();
        }
    }
}
