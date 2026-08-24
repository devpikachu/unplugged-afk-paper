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
import dev.detpikachu.unpluggedafk.network.ProxyManager;
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

    @Override
    public void onEnable() {
        LOGGER = getComponentLogger();

        if (!isTargetMinecraftVersion()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        Options.deserialize(getConfig());

        if (!hasRequiredLink()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerApi();
        registerListeners();
        registerCompat();
        registerPluginChannels();
        startLink();

        logStartupSummary();
    }

    @Override
    public void onDisable() {
        final var registry = SessionRegistry.getInstance();
        final var bots = registry.all();

        getServer().getServicesManager().unregisterAll(this);

        if (!bots.isEmpty()) {
            LOGGER.warn("Disabling with {} bot(s) still active. Their spots will no longer be held.", bots.size());
        }

        bots.forEach(bot -> bot.deferredDisconnect(
                Component.text(KickReasons.DISABLED), UnpluggedPlayerRemoveEvent.Reason.PLUGIN_DISABLED));
        registry.removeAll();

        this.linkClient.stop();
    }

    private boolean isTargetMinecraftVersion() {
        final var targetVersion = getTargetMinecraftVersion();
        final var runningVersion = getServer().getMinecraftVersion();

        if (runningVersion.equals(targetVersion)) {
            return true;
        }

        LOGGER.error("Unplugged AFK targets Minecraft {} but this server runs {}", targetVersion, runningVersion);
        return false;
    }

    private void registerApi() {
        getServer()
                .getServicesManager()
                .register(UnpluggedAfkApi.class, new ApiService(), this, ServicePriority.Normal);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PaperListener(), this);
        getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> CommandTree.register(commands.registrar()));
    }

    private void registerCompat() {
        PacketEventsCompat.register(this);
        HuskSyncCompat.register(this);
        PlaceholderApiCompat.register(this);
    }

    private void registerPluginChannels() {
        final var messenger = getServer().getMessenger();

        messenger.registerOutgoingPluginChannel(this, ProxyManager.BUNGEE_CHANNEL_NAME);
        messenger.registerOutgoingPluginChannel(this, ProxyManager.SESSIONS_CHANNEL_NAME);
    }

    private void logStartupSummary() {
        final var options = Options.getInstance();

        LOGGER.info(
                "Enabled for Minecraft {}. debug={}, maxUnpluggedPlayers={}, maxDurationMins={}",
                getServer().getMinecraftVersion(),
                options.isDebug(),
                options.getMaxUnpluggedPlayers(),
                options.getMaxDurationMins());

        if (GlobalConfiguration.get().proxies.velocity.enabled) {
            LOGGER.info(
                    "Proxy mode is on. /unplug disconnects the player from the proxy, which needs bungee-plugin-message-channel = true in the Velocity config to take effect.");
            return;
        }

        LOGGER.info(
                "Proxy mode is off (proxies.velocity.enabled in paper-global.yml). /unplug only kicks locally, which behind a proxy redirects the player to the try list instead of disconnecting them.");
    }

    private void startLink() {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            return;
        }

        this.linkClient.start();
    }

    private @Nullable String getTargetMinecraftVersion() {
        final var properties = new Properties();

        try (var stream = getResource(PROPERTIES_RESOURCE)) {
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

    private boolean hasRequiredLink() {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            return true;
        }

        if (Options.getInstance().getLink().isValid()) {
            return true;
        }

        LOGGER.error(
                "Proxy mode is on but the proxy link is not configured. Set link.host, link.port (1-65535), link.secret and link.serverName in the plugin's config.yml. The secret and the server name come from the Unplugged AFK companion on your proxy.");
        return false;
    }
}
