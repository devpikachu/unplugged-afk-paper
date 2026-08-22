package dev.detpikachu.unpluggedafk.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.velocity.Constants.SessionsChannel;
import dev.detpikachu.unpluggedafk.velocity.listeners.ProxyListener;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import jakarta.inject.Inject;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "unplugged-afk",
        name = "Unplugged AFK",
        version = BuildConstants.VERSION,
        description = "Routes returning players back to the server holding their unplugged character.",
        url = "https://github.com/devpikachu/unplugged-afk-paper",
        authors = {"Andrei \"detpikachu\" Hava"}
)
@ApiStatus.Internal
public final class UnpluggedAfkVelocity {

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final SessionStore sessionStore;

    @Inject
    public UnpluggedAfkVelocity(Logger logger, ProxyServer proxyServer, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.sessionStore = new SessionStore(dataDirectory, logger);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Sessions
        this.proxyServer.getChannelRegistrar().register(SessionsChannel.IDENTIFIER);
        this.sessionStore.load();

        // Events
        final var listener = new ProxyListener(this.logger, this.proxyServer, this.sessionStore);
        this.proxyServer.getEventManager().register(this, listener);

        this.logger.info("Unplugged AFK has been enabled.");
    }
}
