package dev.detpikachu.unpluggedafk.velocity.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import dev.detpikachu.unpluggedafk.velocity.Constants.SessionsChannel;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

@ApiStatus.Internal
public final class ProxyListener {

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final SessionStore sessionStore;

    public ProxyListener(Logger logger, ProxyServer proxyServer, SessionStore sessionStore) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.sessionStore = sessionStore;
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

    private void handleSessionMessage(ServerConnection source, ByteArrayDataInput in) {
        final var serverName = source.getServerInfo().getName();
        final var message = in.readUTF();

        if (!SessionsChannel.SESSION_START.equals(message)) {
            this.logger.warn("Received unexpected message {} from {}.", message, serverName);
            return;
        }

        final var durationMins = in.readInt();
        final var player = source.getPlayer();

        this.sessionStore.start(player.getUniqueId(), serverName, durationMins);
        this.logger.info(
                "SESSION_START: {} ({}) on {} for {} minute(s)",
                player.getUsername(),
                player.getUniqueId(),
                serverName,
                durationMins);
    }
}
