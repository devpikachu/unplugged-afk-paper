package dev.detpikachu.unpluggedafk.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import jakarta.inject.Inject;
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

    @Inject
    public UnpluggedAfkVelocity(Logger logger, ProxyServer proxyServer) {
        this.logger = logger;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.proxyServer.getChannelRegistrar().register(SESSIONS_CHANNEL);
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

        this.logger.info("SESSION_START: {} on {} for {} minute(s)", source.getPlayer().getUsername(), source.getServerInfo().getName(), in.readInt());
    }
}
