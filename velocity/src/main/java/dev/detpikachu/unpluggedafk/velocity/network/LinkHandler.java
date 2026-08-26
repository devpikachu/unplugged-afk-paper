package dev.detpikachu.unpluggedafk.velocity.network;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.detpikachu.unpluggedafk.common.network.Handshake;
import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.Protocol;
import dev.detpikachu.unpluggedafk.common.network.messages.Auth;
import dev.detpikachu.unpluggedafk.common.network.messages.Challenge;
import dev.detpikachu.unpluggedafk.common.network.messages.Goodbye;
import dev.detpikachu.unpluggedafk.common.network.messages.Heartbeat;
import dev.detpikachu.unpluggedafk.common.network.messages.Ready;
import dev.detpikachu.unpluggedafk.common.network.messages.Relay;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionAck;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionEnd;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionStart;
import dev.detpikachu.unpluggedafk.common.network.messages.Sync;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.compat.tab.TabBridge;
import dev.detpikachu.unpluggedafk.velocity.config.Options;
import dev.detpikachu.unpluggedafk.velocity.session.Session;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity.logDebug;

@ApiStatus.Internal
public final class LinkHandler extends SimpleChannelInboundHandler<Message> {

    private static final String SESSION_HELD_ELSEWHERE = "You already have an unplugged player on another server.";

    private final LinkServer linkServer;
    private final BotPlayerBridge botPlayerBridge;
    private final @Nullable TabBridge tabBridge;
    private final ProxyServer proxyServer;
    private final SessionStore sessionStore;
    private final Logger logger;
    private final String secret;

    private @Nullable String nonce;
    private @Nullable String serverName;

    public LinkHandler(
            LinkServer linkServer,
            UnpluggedAfkVelocity plugin,
            BotPlayerBridge botPlayerBridge,
            @Nullable TabBridge tabBridge) {
        this.linkServer = linkServer;
        this.botPlayerBridge = botPlayerBridge;
        this.tabBridge = tabBridge;
        this.proxyServer = plugin.getProxyServer();
        this.sessionStore = plugin.getSessionStore();
        this.logger = plugin.getLogger();
        this.secret = Options.getInstance().getLink().getSecret();
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        this.nonce = Handshake.newNonce();
        send(context, new Challenge(Protocol.VERSION, this.nonce));
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        final var serverName = this.serverName;

        if (serverName == null) {
            return;
        }

        this.serverName = null;

        if (!this.linkServer.unlinked(serverName, context.channel())) {
            this.logger.info("A superseded link for backend {} closed. Its replacement keeps the backend.", serverName);
            return;
        }

        this.dropBackend(serverName);
        this.logger.info("Backend {} unlinked.", serverName);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        final var serverName = this.serverName;

        if (serverName != null) {
            this.logger.warn("Link error from backend {}. Closing.", serverName, cause);
        }

        close(context);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Message message) {
        if (message instanceof Auth auth) {
            onAuth(context, auth);
            return;
        }

        final var serverName = this.serverName;

        if (serverName == null) {
            refuse(context, "Expected AUTH first but got " + message.getType() + ".");
            return;
        }

        switch (message.getType()) {
            case HEARTBEAT -> {
                logDebug("Heartbeat {} from backend {}, echoing.", ((Heartbeat) message).id(), serverName);
                send(context, message);
            }
            case RELAY -> onRelay((Relay) message, serverName);
            case SESSION_START -> onSessionStart(context, (SessionStart) message, serverName);
            case SESSION_END -> onSessionEnd((SessionEnd) message, serverName);
            case SYNC -> onSync((Sync) message, serverName);
            case GOODBYE -> onGoodbye(context, (Goodbye) message, serverName);
            case AUTH, CHALLENGE, READY, SESSION_ACK ->
                logDebug("Ignoring {} from backend {}. A proxy never handles it.", message.getType(), serverName);
        }
    }

    private void onGoodbye(ChannelHandlerContext context, Goodbye message, String serverName) {
        this.logger.info("Backend {} said goodbye: {}", serverName, message.reason());

        this.serverName = null;

        if (this.linkServer.unlinked(serverName, context.channel())) {
            this.dropBackend(serverName);
        }

        close(context);
    }

    private void onAuth(ChannelHandlerContext context, Auth auth) {
        if (this.serverName != null) {
            refuse(context, "Backend " + this.serverName + " is already authenticated on this connection.");
            return;
        }

        if (auth.protocolVersion() != Protocol.VERSION) {
            refuse(context, "Protocol version mismatch. Ensure both plugins are the same version.");
            return;
        }

        if (this.nonce == null || !Handshake.verify(this.secret, this.nonce, auth.signature())) {
            refuse(context, "The backend's link.secret does not match this proxy's.");
            return;
        }

        if (this.linkServer.isLinked(auth.serverName())) {
            refuse(
                    context,
                    "Backend " + auth.serverName()
                            + " is already linked from another connection. Two servers must not share a link.serverName.");
            return;
        }

        if (this.proxyServer.getServer(auth.serverName()).isEmpty()) {
            refuse(
                    context,
                    "No server named "
                            + auth.serverName()
                            + " is registered on this proxy. Registered: "
                            + registeredNames()
                            + ".");
            return;
        }

        this.nonce = null;
        this.serverName = auth.serverName();
        context.pipeline()
                .replace(ReadTimeoutHandler.class, "timeout", new ReadTimeoutHandler(Protocol.IDLE_TIMEOUT_SECS));
        this.linkServer.linked(auth.serverName(), context.channel());
        send(context, new Ready(true, ""));
        this.logger.info(
                "Backend {} linked from {}.",
                auth.serverName(),
                context.channel().remoteAddress());
    }

    private void onRelay(Relay relay, String serverName) {
        final var identifier = parseChannel(relay.getChannel());

        if (identifier == null) {
            this.logger.warn(
                    "Dropped a relayed message from {} on the invalid channel {}.", serverName, relay.getChannel());
            return;
        }

        final var source = this.botPlayerBridge.connectionOf(relay.getUuid());

        if (source == null) {
            this.logger.warn(
                    "Dropped a relayed message from {}: bot {} has no proxy connection.", serverName, relay.getUuid());
            return;
        }

        logDebug(
                "Relaying {} byte(s) from bot {} on {} to the proxy on channel {}.",
                relay.getPayload().length,
                relay.getUuid(),
                serverName,
                relay.getChannel());
        this.proxyServer
                .getEventManager()
                .fireAndForget(new PluginMessageEvent(source, source, identifier, relay.getPayload()));
    }

    private void onSessionStart(ChannelHandlerContext context, SessionStart start, String serverName) {
        final var uuid = start.uuid();
        final var username = start.username();
        final var session = toSession(serverName, start);

        if (!this.sessionStore.start(uuid, session)) {
            send(context, new SessionAck(uuid, false, SESSION_HELD_ELSEWHERE));
            return;
        }

        this.addPresence(serverName, uuid, username, session.skin());
        send(context, new SessionAck(uuid, true, ""));
        this.logger.info(
                "SESSION_START: {} ({}) on {} for {} second(s).", username, uuid, serverName, start.secondsRemaining());
    }

    private void onSessionEnd(SessionEnd end, String serverName) {
        final var uuid = end.uuid();

        if (this.sessionStore.isHeldElsewhere(uuid, serverName)) {
            logDebug("Ignored a SESSION_END for {} from {}: another backend holds it.", uuid, serverName);
            return;
        }

        this.sessionStore.end(serverName, uuid);
        this.dropPresence(uuid);
        this.logger.info("SESSION_END: {}: {}", uuid, end.reason());
    }

    private void onSync(Sync sync, String serverName) {
        final var sessions = new HashMap<UUID, Session>();

        for (final var start : sync.sessions()) {
            final var previous = sessions.get(start.uuid());

            if (previous != null && previous.isAlive() && start.secondsRemaining() <= 0) {
                continue;
            }

            sessions.put(start.uuid(), toSession(serverName, start));
        }

        this.sessionStore.replace(serverName, sessions);
        this.clearPresence(serverName);

        for (final var start : sync.sessions()) {
            if (start.secondsRemaining() <= 0) {
                continue;
            }

            this.addPresence(serverName, start.uuid(), start.username(), skinOf(start.skin()));
        }

        this.logger.info("SYNC from {}: {} session(s).", serverName, sessions.size());
    }

    private void addPresence(String serverName, UUID uuid, String username, Session.@Nullable Skin skin) {
        logDebug("Adding presence for {} ({}) on {}. Skin: {}.", username, uuid, serverName, skin != null);
        this.botPlayerBridge.addWhenDisconnected(serverName, uuid, username, skin);

        if (this.tabBridge != null) {
            this.tabBridge.addBot(serverName, uuid, username, skin);
        }
    }

    private void dropPresence(UUID uuid) {
        logDebug("Dropping presence for {}.", uuid);
        this.botPlayerBridge.remove(uuid);

        if (this.tabBridge != null) {
            this.tabBridge.removeBot(uuid);
        }
    }

    private void clearPresence(String serverName) {
        this.botPlayerBridge.removeServer(serverName).forEach(this::dropPresence);
    }

    private void dropBackend(String serverName) {
        this.clearPresence(serverName);
        this.sessionStore.dropServer(serverName);
    }

    private void refuse(ChannelHandlerContext context, String reason) {
        this.logger.warn("Refusing a link from {}. {}", context.channel().remoteAddress(), reason);
        sendAndClose(context, new Ready(false, reason));
    }

    private String registeredNames() {
        return this.proxyServer.getAllServers().stream()
                .map(server -> server.getServerInfo().getName())
                .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void send(ChannelHandlerContext context, Message message) {
        context.writeAndFlush(message);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void sendAndClose(ChannelHandlerContext context, Message message) {
        context.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void close(ChannelHandlerContext context) {
        context.close();
    }

    private static @Nullable ChannelIdentifier parseChannel(String channel) {
        try {
            return MinecraftChannelIdentifier.from(channel);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Session toSession(String serverName, SessionStart start) {
        final var now = Instant.now();

        return new Session(
                serverName,
                start.username(),
                skinOf(start.skin()),
                start.durationMins(),
                start.reason(),
                now.minusSeconds(start.durationMins() * 60L - start.secondsRemaining()),
                now.plusSeconds(start.secondsRemaining()));
    }

    private static Session.@Nullable Skin skinOf(SessionStart.@Nullable Skin skin) {
        return skin == null ? null : new Session.Skin(skin.value(), skin.signature());
    }
}
