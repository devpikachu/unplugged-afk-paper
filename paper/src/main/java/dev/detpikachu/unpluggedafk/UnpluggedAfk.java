package dev.detpikachu.unpluggedafk;

import dev.detpikachu.unpluggedafk.api.UnpluggedAfkApi;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent;
import dev.detpikachu.unpluggedafk.commands.CommandTree;
import dev.detpikachu.unpluggedafk.compat.husksync.HuskSyncCompat;
import dev.detpikachu.unpluggedafk.compat.packetevents.PacketEventsCompat;
import dev.detpikachu.unpluggedafk.compat.placeholderapi.PlaceholderApiCompat;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.listeners.PaperListener;
import dev.detpikachu.unpluggedafk.network.LinkClient;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Properties;

@ApiStatus.Internal
public final class UnpluggedAfk extends JavaPlugin {

    private static final String PROPERTIES_RESOURCE = "unplugged-afk.properties";
    private static final String KEY_MINECRAFT_VERSION = "minecraftVersion";

    public static ComponentLogger LOGGER = ComponentLogger.logger();

    private final LinkClient linkClient = new LinkClient();

    public static UnpluggedAfk getInstance() {
        return JavaPlugin.getPlugin(UnpluggedAfk.class);
    }

    public static void logDebug(String message, Object... arguments) {
        if (Options.getInstance().isDebug()) {
            LOGGER.info(message, arguments);
        }
    }

    public static boolean isProxyMode() {
        return GlobalConfiguration.get().proxies.velocity.enabled;
    }

    public LinkClient getLinkClient() {
        return this.linkClient;
    }

    @Override
    public void onEnable() {
        LOGGER = getComponentLogger();
        this.warnOnVersionMismatch();

        this.saveDefaultConfig();
        Options.deserialize(this.getConfig());

        if (!this.hasRequiredLink()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.registerApi();
        this.registerListeners();
        this.registerCompat();
        this.startLink();

        this.logStartupSummary();
    }

    @Override
    public void onDisable() {
        final var registry = SessionRegistry.getInstance();
        final var bots = registry.all();

        this.getServer().getServicesManager().unregisterAll(this);

        if (!bots.isEmpty()) {
            LOGGER.warn("Disabling with {} bot(s) still active. Their spots will no longer be held.", bots.size());
        }

        bots.forEach(bot -> bot.deferredDisconnect(
                Component.text(KickReasons.DISABLED), UnpluggedPlayerRemoveEvent.Reason.PLUGIN_DISABLED));
        registry.removeAll();

        this.linkClient.stop();
    }

    private void registerApi() {
        this.getServer()
                .getServicesManager()
                .register(UnpluggedAfkApi.class, new ApiService(), this, ServicePriority.Normal);
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new PaperListener(), this);
        this.getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> CommandTree.register(commands.registrar()));
    }

    private void registerCompat() {
        PacketEventsCompat.register(this);
        HuskSyncCompat.register(this);
        PlaceholderApiCompat.register(this);
    }

    private void logStartupSummary() {
        final var options = Options.getInstance();

        LOGGER.info(
                "Enabled for Minecraft {}. maxUnpluggedPlayers={}, maxDurationMins={}",
                this.getServer().getMinecraftVersion(),
                options.getMaxUnpluggedPlayers(),
                options.getMaxDurationMins());

        if (isProxyMode()) {
            final var link = options.getLink();

            LOGGER.info(
                    "Proxy mode is on. Sessions ride the link to {}:{} as {}.",
                    link.getHost(),
                    link.getPort(),
                    link.getServerName());
            return;
        }

        LOGGER.info(
                "Proxy mode is off (proxies.velocity.enabled in paper-global.yml). /unplug only kicks locally, which behind a proxy redirects the player to the try list instead of disconnecting them.");
    }

    private void startLink() {
        if (!isProxyMode()) {
            return;
        }

        this.linkClient.start();
    }

    private boolean hasRequiredLink() {
        if (!isProxyMode()) {
            return true;
        }

        if (Options.getInstance().getLink().isValid()) {
            return true;
        }

        LOGGER.error(
                "Proxy mode is on but the proxy link is not configured. Set link.secret and link.serverName in the plugin's config.yml, plus link.host if the proxy is not on this machine. The secret comes from the Unplugged AFK companion on your proxy, and the server name is this server's name in the proxy's own configuration.");
        return false;
    }

    private void warnOnVersionMismatch() {
        final var targetVersion = this.getTargetMinecraftVersion();
        final var runningVersion = this.getServer().getMinecraftVersion();

        if (runningVersion.equals(targetVersion)) {
            return;
        }

        LOGGER.warn(
                "Unplugged AFK targets Minecraft {} but this server runs {}. It relies on server internals, so features may misbehave or fail outright on another version.",
                targetVersion,
                runningVersion);
    }

    private @Nullable String getTargetMinecraftVersion() {
        final var properties = new Properties();

        try (var stream = this.getResource(PROPERTIES_RESOURCE)) {
            if (stream == null) {
                LOGGER.error("{} is missing from the JAR.", PROPERTIES_RESOURCE);
                return null;
            }

            properties.load(stream);
        } catch (IOException exception) {
            LOGGER.error("Could not read {} from the JAR.", PROPERTIES_RESOURCE, exception);
            return null;
        }

        return properties.getProperty(KEY_MINECRAFT_VERSION);
    }
}
