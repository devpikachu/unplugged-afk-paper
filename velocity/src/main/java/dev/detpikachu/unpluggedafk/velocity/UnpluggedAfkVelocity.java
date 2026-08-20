package dev.detpikachu.unpluggedafk.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import jakarta.inject.Inject;
import java.nio.file.Path;
import org.slf4j.Logger;

import static dev.detpikachu.unpluggedafk.velocity.UnpluggedConstants.CHANNEL_SESSIONS;
import static dev.detpikachu.unpluggedafk.velocity.UnpluggedConstants.MESSAGE_SESSION_START;

@Plugin(
        id = "unplugged-afk",
        name = "Unplugged AFK",
        version = BuildConstants.VERSION,
        description = "Routes returning players back to the server holding their unplugged character.",
        url = "https://github.com/devpikachu/unplugged-afk-paper",
        authors = {"Andrei \"detpikachu\" Hava"}
)
public final class UnpluggedAfkVelocity {

    private static final MinecraftChannelIdentifier SESSIONS_CHANNEL = MinecraftChannelIdentifier.from(CHANNEL_SESSIONS);

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final UnpluggedSessionStore sessionStore;

    @Inject
    public UnpluggedAfkVelocity(Logger logger, ProxyServer proxyServer, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.sessionStore = new UnpluggedSessionStore(dataDirectory, logger);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.proxyServer.getChannelRegistrar().register(SESSIONS_CHANNEL);
        this.sessionStore.load();
        this.logger.info("Unplugged AFK has been enabled.");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!SESSIONS_CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection source)) {
            return;
        }

        final var in = event.dataAsDataStream();
        final var message = in.readUTF();

        if (!MESSAGE_SESSION_START.equals(message)) {
            this.logger.warn("Received unexpected message {} from server.", message);
            return;
        }

        final var durationMins = in.readInt();
        final var player = source.getPlayer();
        final var serverName = source.getServerInfo().getName();

        this.sessionStore.start(player.getUniqueId(), serverName, durationMins);
        this.logger.info("SESSION_START: {} on {} for {} minute(s)", player.getUsername(), serverName, durationMins);
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
            this.logger.warn("{} unplugged on {}, which is no longer registered - falling back to the try list.", player.getUsername(), serverName);
            return;
        }

        event.setInitialServer(server.get());
        this.logger.info("Routing {} back to {}", player.getUsername(), serverName);
    }
}
