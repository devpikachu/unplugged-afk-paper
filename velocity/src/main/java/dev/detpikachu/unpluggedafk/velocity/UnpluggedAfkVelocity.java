package dev.detpikachu.unpluggedafk.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.velocity.compat.tab.TabCompat;
import dev.detpikachu.unpluggedafk.velocity.config.Options;
import dev.detpikachu.unpluggedafk.velocity.listeners.ProxyListener;
import dev.detpikachu.unpluggedafk.velocity.network.LinkServer;
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
        authors = {"Andrei \"detpikachu\" Hava"},
        dependencies = {@Dependency(id = "tab", optional = true)})
@ApiStatus.Internal
public final class UnpluggedAfkVelocity {

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final SessionStore sessionStore;
    private final Path dataDirectory;
    private final LinkServer linkServer;

    @Inject
    public UnpluggedAfkVelocity(Logger logger, ProxyServer proxyServer, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.sessionStore = new SessionStore(dataDirectory, logger);
        this.dataDirectory = dataDirectory;
        this.linkServer = new LinkServer(proxyServer, logger);
    }

    public Logger getLogger() {
        return this.logger;
    }

    public ProxyServer getProxyServer() {
        return this.proxyServer;
    }

    public SessionStore getSessionStore() {
        return this.sessionStore;
    }

    public Path getDataDirectory() {
        return this.dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Link
        Options.deserialize(this.dataDirectory, this.logger);
        this.linkServer.start();

        // Sessions
        ProxyListener.register(this);
        this.sessionStore.load();

        // Compatibility
        final var tabBridge = TabCompat.register(this);

        // Events
        final var listener = new ProxyListener(this, tabBridge);
        this.proxyServer.getEventManager().register(this, listener);

        final var linkOptions = Options.getInstance().getLink();
        this.logger.info(
                "Unplugged AFK has been enabled. Link configured for {}:{}.",
                linkOptions.getHost(),
                linkOptions.getPort());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        this.linkServer.stop();
    }
}
