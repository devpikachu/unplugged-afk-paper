package dev.detpikachu.unpluggedafk.velocity.network;

import com.velocitypowered.api.network.HandshakeIntent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.session.Session;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity.logDebug;

@ApiStatus.Internal
public final class BotPlayerBridge {

    private static final String TEXTURES_PROPERTY = "textures";

    private static final InetSocketAddress BOT_ADDRESS = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

    private static final String LOCATOR_VELOCITY_SERVER = "com.velocitypowered.proxy.VelocityServer";
    private static final String LOCATOR_MINECRAFT_CONNECTION =
            "com.velocitypowered.proxy.connection.MinecraftConnection";
    private static final String LOCATOR_CONNECTED_PLAYER =
            "com.velocitypowered.proxy.connection.client.ConnectedPlayer";
    private static final String LOCATOR_VELOCITY_SERVER_CONNECTION =
            "com.velocitypowered.proxy.connection.backend.VelocityServerConnection";
    private static final String LOCATOR_VELOCITY_REGISTERED_SERVER =
            "com.velocitypowered.proxy.server.VelocityRegisteredServer";
    private static final String LOCATOR_PLUGIN_MESSAGE_PACKET =
            "com.velocitypowered.proxy.protocol.packet.PluginMessagePacket";

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final LinkServer linkServer;

    private final Constructor<?> minecraftConnection;
    private final Field protocolVersion;
    private final Constructor<?> connectedPlayer;
    private final Method registerConnection;
    private final Method unregisterConnection;
    private final Constructor<?> serverConnection;
    private final Field backendConnection;
    private final Method setConnectedServer;
    private final Method addPlayer;
    private final Method removePlayer;
    private final Class<?> pluginMessagePacket;
    private final Method packetChannel;

    private final ConcurrentHashMap<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Bot> bots = new ConcurrentHashMap<>();

    public BotPlayerBridge(UnpluggedAfkVelocity plugin) throws ReflectiveOperationException {
        final var velocityServerClass = Class.forName(LOCATOR_VELOCITY_SERVER);
        final var minecraftConnectionClass = Class.forName(LOCATOR_MINECRAFT_CONNECTION);
        final var connectedPlayerClass = Class.forName(LOCATOR_CONNECTED_PLAYER);
        final var serverConnectionClass = Class.forName(LOCATOR_VELOCITY_SERVER_CONNECTION);
        final var registeredServerClass = Class.forName(LOCATOR_VELOCITY_REGISTERED_SERVER);
        final var pluginMessagePacketClass = Class.forName(LOCATOR_PLUGIN_MESSAGE_PACKET);

        this.proxyServer = plugin.getProxyServer();
        this.logger = plugin.getLogger();
        this.linkServer = plugin.getLinkServer();

        this.minecraftConnection = minecraftConnectionClass.getConstructor(Channel.class, velocityServerClass);
        this.protocolVersion = minecraftConnectionClass.getDeclaredField("protocolVersion");
        this.protocolVersion.setAccessible(true);
        this.connectedPlayer = connectedPlayerClass.getDeclaredConstructor(
                velocityServerClass,
                GameProfile.class,
                minecraftConnectionClass,
                InetSocketAddress.class,
                String.class,
                boolean.class,
                HandshakeIntent.class,
                IdentifiedKey.class);
        this.connectedPlayer.setAccessible(true);
        this.registerConnection = velocityServerClass.getMethod("registerConnection", connectedPlayerClass);
        this.unregisterConnection = velocityServerClass.getMethod("unregisterConnection", connectedPlayerClass);
        this.serverConnection = serverConnectionClass.getConstructor(
                registeredServerClass, registeredServerClass, connectedPlayerClass, velocityServerClass);
        this.backendConnection = serverConnectionClass.getDeclaredField("connection");
        this.backendConnection.setAccessible(true);
        this.setConnectedServer = connectedPlayerClass.getMethod("setConnectedServer", serverConnectionClass);
        this.addPlayer = registeredServerClass.getMethod("addPlayer", connectedPlayerClass);
        this.removePlayer = registeredServerClass.getMethod("removePlayer", connectedPlayerClass);
        this.pluginMessagePacket = pluginMessagePacketClass;
        this.packetChannel = pluginMessagePacketClass.getMethod("getChannel");
    }

    public void addWhenDisconnected(String serverName, UUID uuid, String username, Session.@Nullable Skin skin) {
        this.pending.put(uuid, new Pending(serverName, username, skin));

        final var stillConnected = this.proxyServer.getPlayer(uuid).isPresent();
        logDebug(
                "Presence for {} ({}) on {} is pending. Real player still connected: {}.",
                username,
                uuid,
                serverName,
                stillConnected);

        if (!stillConnected) {
            this.addPending(uuid);
        }
    }

    public void addPending(UUID uuid) {
        final var details = this.pending.remove(uuid);

        if (details == null) {
            logDebug("No pending presence for {}, so nothing to materialise.", uuid);
            return;
        }

        if (this.bots.containsKey(uuid)) {
            logDebug("Presence for {} ({}) already exists, so it is left alone.", details.username(), uuid);
            return;
        }

        final var serverName = details.serverName();
        final var username = details.username();
        final var server = this.proxyServer.getServer(serverName);

        if (server.isEmpty()) {
            this.logger.warn(
                    "Cannot give bot {} ({}) a proxy connection: no server named {} is registered.",
                    username,
                    uuid,
                    serverName);
            return;
        }

        try {
            final var bot = (Player) this.connectedPlayer.newInstance(
                    this.proxyServer,
                    profileOf(uuid, username, details.skin()),
                    this.newConnection(new DiscardHandler()),
                    null,
                    null,
                    true,
                    HandshakeIntent.LOGIN,
                    null);

            final var connection = this.serverConnection.newInstance(server.get(), null, bot, this.proxyServer);
            this.backendConnection.set(connection, this.newConnection(new RelayHandler(serverName, uuid)));
            this.setConnectedServer.invoke(bot, connection);

            if (!Boolean.TRUE.equals(this.registerConnection.invoke(this.proxyServer, bot))) {
                this.logger.warn(
                        "The proxy would not register bot {} ({}). Some functionality will be disabled.",
                        username,
                        uuid);
                return;
            }

            this.bots.put(uuid, new Bot(bot, (ServerConnection) connection, server.get()));
            this.addPlayer.invoke(server.get(), bot);
            logDebug("Gave bot {} ({}) a proxy connection on {}.", username, uuid, serverName);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            this.logger.warn(
                    "Could not register bot {} ({}) with the proxy. Some functionality will be disabled.",
                    username,
                    uuid,
                    exception);
        }
    }

    public void remove(UUID uuid) {
        final var wasPending = this.pending.remove(uuid) != null;
        final var bot = this.bots.remove(uuid);

        if (wasPending || bot != null) {
            logDebug("Dropped presence for {}. Was pending: {}. Had a connection: {}.", uuid, wasPending, bot != null);
        }

        drop(bot);
    }

    public void remove(String username) {
        this.pending.values().removeIf(details -> details.username().equalsIgnoreCase(username));
        for (final var bot : this.bots.values()) {
            if (bot.player().getUsername().equalsIgnoreCase(username)) {
                remove(bot.player().getUniqueId());
            }
        }
    }

    public List<UUID> removeServer(String serverName) {
        final var removed = new ArrayList<UUID>();

        this.pending.values().removeIf(details -> details.serverName().equals(serverName));

        for (final var entry : this.bots.entrySet()) {
            if (entry.getValue().server().getServerInfo().getName().equals(serverName)) {
                removed.add(entry.getKey());
            }
        }

        removed.forEach(this::remove);
        return removed;
    }

    public @Nullable ServerConnection connectionOf(UUID uuid) {
        final var bot = this.bots.get(uuid);
        return bot == null ? null : bot.connection();
    }

    private Object newConnection(ChannelOutboundHandlerAdapter drain) throws ReflectiveOperationException {
        final var connection = this.minecraftConnection.newInstance(new BotChannel(drain), this.proxyServer);
        this.protocolVersion.set(connection, ProtocolVersion.MAXIMUM_VERSION);

        return connection;
    }

    private void drop(@Nullable Bot bot) {
        if (bot == null) {
            return;
        }

        try {
            this.removePlayer.invoke(bot.server(), bot.player());
            this.unregisterConnection.invoke(this.proxyServer, bot.player());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            this.logger.warn(
                    "Could not clear bot {} from the proxy.", bot.player().getUsername(), exception);
        }
    }

    private static GameProfile profileOf(UUID uuid, String username, Session.@Nullable Skin skin) {
        if (skin == null) {
            return new GameProfile(uuid, username, List.of());
        }

        final var signature = skin.signature();
        final var textures =
                new GameProfile.Property(TEXTURES_PROPERTY, skin.value(), signature == null ? "" : signature);

        return new GameProfile(uuid, username, List.of(textures));
    }

    private record Pending(String serverName, String username, Session.@Nullable Skin skin) {}

    private record Bot(Player player, ServerConnection connection, RegisteredServer server) {}

    private static final class BotChannel extends EmbeddedChannel {

        private BotChannel(ChannelOutboundHandlerAdapter drain) {
            super(drain);
        }

        @Override
        protected SocketAddress remoteAddress0() {
            return BOT_ADDRESS;
        }

        @Override
        protected SocketAddress localAddress0() {
            return BOT_ADDRESS;
        }
    }

    private static class DiscardHandler extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
            ReferenceCountUtil.release(message);
            promise.trySuccess();
        }

        @Override
        public void flush(ChannelHandlerContext context) {}
    }

    private final class RelayHandler extends DiscardHandler {

        private final String serverName;
        private final UUID uuid;

        private RelayHandler(String serverName, UUID uuid) {
            this.serverName = serverName;
            this.uuid = uuid;
        }

        @Override
        public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
            relay(message);
            super.write(context, message, promise);
        }

        private void relay(Object message) {
            if (!pluginMessagePacket.isInstance(message)) {
                return;
            }

            try {
                final var payload = ByteBufUtil.getBytes(((ByteBufHolder) message).content());
                linkServer.relay(this.serverName, this.uuid, (String) packetChannel.invoke(message), payload);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                logger.warn("Could not relay a message to bot {} on {}.", this.uuid, this.serverName, exception);
            }
        }
    }
}
