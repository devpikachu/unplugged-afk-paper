package dev.detpikachu.unpluggedafk.velocity.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import dev.detpikachu.unpluggedafk.velocity.Constants.SessionsChannel;
import dev.detpikachu.unpluggedafk.velocity.compat.tab.TabBridge;
import dev.detpikachu.unpluggedafk.velocity.session.Session;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@ApiStatus.Internal
public final class ProxyListener {

    private static final String TEXTURES_PROPERTY = "textures";

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final SessionStore sessionStore;
    private final @Nullable TabBridge tabBridge;

    public ProxyListener(
            Logger logger,
            ProxyServer proxyServer,
            SessionStore sessionStore,
            @Nullable TabBridge tabBridge) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.sessionStore = sessionStore;
        this.tabBridge = tabBridge;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!SessionsChannel.IDENTIFIER.equals(event.getIdentifier())) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection source)) {
            final var origin = event.getSource() instanceof Player player ? player.getUsername() : "an unknown source";

            this.logger.warn(
                    "Ignoring a {} message from {}. Only a backend server may start a session, so this may be a client trying to pin itself to a server.",
                    SessionsChannel.NAME,
                    origin);
            return;
        }

        try {
            this.handleSessionMessage(source, event.dataAsDataStream());
        } catch (RuntimeException exception) {
            this.logger.warn(
                    "Could not read a {} message from {}. That backend may be running a different version of the plugin.",
                    SessionsChannel.NAME,
                    source.getServerInfo().getName(),
                    exception);
        }
    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        final var player = event.getPlayer();

        if (this.tabBridge != null) {
            this.tabBridge.removeBot(player.getUniqueId());
        }

        final var session = this.sessionStore.consume(player.getUniqueId());

        if (session.isEmpty()) {
            return;
        }

        final var serverName = session.get().serverName();
        final var server = this.proxyServer.getServer(serverName);

        if (server.isEmpty()) {
            this.logger.warn(
                    "{} unplugged on {}, which is no longer registered, so they fall back to the try list.",
                    player.getUsername(),
                    serverName);
            return;
        }

        event.setInitialServer(server.get());
        this.logger.info("Routing {} back to {}", player.getUsername(), serverName);
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        final var bots = this.sessionStore.count();

        if (bots == 0) {
            return;
        }

        // A ping carrying no players section at all cannot carry a count either. asBuilder() records that as
        // nullOutPlayers and build() then throws away whatever onlinePlayers() was told, so rebuilding here would
        // be a silent no-op rather than an error.
        if (event.getPing().getPlayers().isEmpty()) {
            return;
        }

        final var builder = event.getPing().asBuilder();
        event.setPing(builder.onlinePlayers(builder.getOnlinePlayers() + bots).build());
    }

    private void handleSessionMessage(ServerConnection source, ByteArrayDataInput in) {
        final var serverName = source.getServerInfo().getName();
        final var message = in.readUTF();

        if (!SessionsChannel.SESSION_START.equals(message)) {
            this.logger.warn("Received unexpected message {} from {}.", message, serverName);
            return;
        }

        final var durationMins = in.readInt();
        final var player = source.getPlayer();
        final var uuid = player.getUniqueId();
        final var username = player.getUsername();
        final var skin = skinOf(player);

        this.sessionStore.start(serverName, uuid, username, skin, durationMins);

        if (this.tabBridge != null) {
            this.tabBridge.addBot(serverName, uuid, username, skin);
        }

        this.logger.info(
                "SESSION_START: {} ({}) on {} for {} minute(s)",
                player.getUsername(),
                player.getUniqueId(),
                serverName,
                durationMins);
    }

    private static Session.@Nullable Skin skinOf(Player player) {
        for (final var property : player.getGameProfile().getProperties()) {
            if (TEXTURES_PROPERTY.equals(property.getName())) {
                return new Session.Skin(property.getValue(), property.getSignature());
            }
        }

        return null;
    }
}
