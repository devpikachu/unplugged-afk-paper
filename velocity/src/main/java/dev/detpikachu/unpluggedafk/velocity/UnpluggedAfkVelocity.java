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
import dev.detpikachu.unpluggedafk.velocity.network.BotPlayerBridge;
import dev.detpikachu.unpluggedafk.velocity.network.LinkServer;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import jakarta.inject.Inject;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static Logger LOGGER = LoggerFactory.getLogger("unplugged-afk");

    private final Logger logger;
    private final ProxyServer proxyServer;
    private final SessionStore sessionStore;
    private final Path dataDirectory;
    private final LinkServer linkServer;

    @Inject
    public UnpluggedAfkVelocity(Logger logger, ProxyServer proxyServer, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.sessionStore = new SessionStore(logger);
        this.dataDirectory = dataDirectory;
        this.linkServer = new LinkServer(logger);
    }

    public static void logDebug(String message, Object... arguments) {
        if (Options.getInstance().isDebug()) {
            LOGGER.info(message, arguments);
        }
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

    public LinkServer getLinkServer() {
        return this.linkServer;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        LOGGER = this.logger;

        final BotPlayerBridge botPlayerBridge;

        try {
            botPlayerBridge = new BotPlayerBridge(this);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error(
                    "Unplugged AFK could not resolve Velocity's internals, which it needs to give an unplugged player a proxy connection. Ensure you are using a version of Velocity that is supported by this version of the plugin.",
                    exception);
            return;
        }

        Options.deserialize(this.dataDirectory, LOGGER);

        final var tabBridge = TabCompat.register(this);

        this.linkServer.start(this, botPlayerBridge, tabBridge);

        final var listener = new ProxyListener(this, botPlayerBridge, tabBridge);
        this.proxyServer.getEventManager().register(this, listener);

        final var linkOptions = Options.getInstance().getLink();
        LOGGER.info(
                "Unplugged AFK has been enabled. Link listening on {}:{}.",
                linkOptions.getHost(),
                linkOptions.getPort());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        this.linkServer.stop();
    }
}
