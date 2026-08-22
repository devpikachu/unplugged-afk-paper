package dev.detpikachu.unpluggedafk;

import dev.detpikachu.unpluggedafk.Constants.BungeeChannel;
import dev.detpikachu.unpluggedafk.Constants.KickReasons;
import dev.detpikachu.unpluggedafk.Constants.SessionsChannel;
import dev.detpikachu.unpluggedafk.api.UnpluggedAfkApi;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent;
import dev.detpikachu.unpluggedafk.commands.CommandTree;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.listeners.PaperListener;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
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

    public static void logDebug(String message, Object... arguments) {
        if (Options.getInstance().isDebug()) {
            LOGGER.info(message, arguments);
        }
    }

    @Override
    public void onEnable() {
        LOGGER = getComponentLogger();
        final var server = getServer();

        // Version compatibility
        final var targetVersion = getTargetMinecraftVersion();
        final var runningVersion = Bukkit.getMinecraftVersion();

        if (targetVersion != null && !targetVersion.equals(runningVersion)) {
            LOGGER.error("unplugged-afk targets Minecraft {} but this server runs {}", targetVersion, runningVersion);
            server.getPluginManager().disablePlugin(this);
            return;
        }

        // Config
        saveDefaultConfig();
        Options.deserialize(getConfig());

        // API
        server.getServicesManager().register(UnpluggedAfkApi.class, new ApiService(), this, ServicePriority.Normal);

        // Events
        Bukkit.getPluginManager().registerEvents(new PaperListener(), this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            CommandTree.register(commands.registrar());
        });

        // Networking
        final var messenger = server.getMessenger();
        messenger.registerOutgoingPluginChannel(this, BungeeChannel.NAME);
        messenger.registerOutgoingPluginChannel(this, SessionsChannel.NAME);

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

        bots.forEach(bot -> bot.deferredDisconnect(Component.text(KickReasons.DISABLED), UnpluggedPlayerRemoveEvent.Reason.PLUGIN_DISABLED));
        registry.removeAll();
    }

    private void logStartupSummary() {
        final var options = Options.getInstance();

        LOGGER.info(
                "Enabled for Minecraft {}. debug={}, maxUnpluggedPlayers={}, maxDurationMins={}",
                Bukkit.getMinecraftVersion(),
                options.isDebug(),
                options.getMaxUnpluggedPlayers(),
                options.getMaxDurationMins()
        );

        if (GlobalConfiguration.get().proxies.velocity.enabled) {
            LOGGER.info("Proxy mode is on. /unplug disconnects the player from the proxy, which needs bungee-plugin-message-channel = true in the Velocity config to take effect.");
            return;
        }

        LOGGER.info("Proxy mode is off (proxies.velocity.enabled in paper-global.yml). /unplug only kicks locally, which behind a proxy redirects the player to the try list instead of disconnecting them.");
    }

    private @Nullable String getTargetMinecraftVersion() {
        final var properties = new Properties();

        try (var stream = getResource(PROPERTIES_RESOURCE)) {
            if (stream == null) {
                return null;
            }

            properties.load(stream);
        } catch (IOException exception) {
            return null;
        }

        return properties.getProperty(KEY_MINECRAFT_VERSION);
    }
}
