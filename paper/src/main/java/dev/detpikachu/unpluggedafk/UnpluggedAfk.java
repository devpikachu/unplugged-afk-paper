package dev.detpikachu.unpluggedafk;

import dev.detpikachu.unpluggedafk.commands.UnpluggedCommands;
import dev.detpikachu.unpluggedafk.config.UnpluggedOptions;
import dev.detpikachu.unpluggedafk.formatting.UnpluggedChatFormatting;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.util.Properties;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.Component;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static dev.detpikachu.unpluggedafk.UnpluggedConstants.CHANNEL_BUNGEE;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.CHANNEL_SESSIONS;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.KICK_REASON_DISABLED;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.KICK_REASON_PACKETEVENTS;

public final class UnpluggedAfk extends JavaPlugin implements Listener {

    private static final String PROPERTIES_RESOURCE = "unplugged-afk.properties";
    private static final String KEY_MINECRAFT_VERSION = "minecraftVersion";

    public static ComponentLogger LOGGER;

    public static void logDebug(String message, Object... arguments) {
        if (UnpluggedOptions.getInstance().isDebug()) {
            LOGGER.info(message, arguments);
        }
    }

    @Override
    public void onEnable() {
        LOGGER = getComponentLogger();

        // Version compatibility
        final var targetVersion = getTargetMinecraftVersion();
        final var runningVersion = Bukkit.getMinecraftVersion();

        if (targetVersion != null && !targetVersion.equals(runningVersion)) {
            LOGGER.error("unplugged-afk targets Minecraft {} but this server runs {}", targetVersion, runningVersion);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Config
        saveDefaultConfig();
        UnpluggedOptions.deserialize(getConfig());

        // Events
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            UnpluggedCommands.register(commands.registrar());
        });

        // Networking
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL_BUNGEE);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL_SESSIONS);

        logStartupSummary();
    }

    @Override
    public void onDisable() {
        final var manager = UnpluggedPlayerManager.getInstance();
        final var unpluggedPlayers = manager.getPlayers();

        if (!unpluggedPlayers.isEmpty()) {
            LOGGER.warn("Disabling with {} unplugged player(s) still active. Their spots will no longer be held.", unpluggedPlayers.size());
        }

        unpluggedPlayers.forEach(unpluggedPlayer -> unpluggedPlayer.deferredDisconnect(Component.literal(KICK_REASON_DISABLED)));
        manager.removeAll();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (((CraftPlayer) event.getPlayer()).getHandle() instanceof UnpluggedServerPlayer) {
            event.joinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (((CraftPlayer) event.getPlayer()).getHandle() instanceof UnpluggedServerPlayer unpluggedPlayer) {
            UnpluggedPlayerManager.getInstance().remove(unpluggedPlayer);
            logDebug("Removed unplugged player {} ({}). {} still active.", event.getPlayer().getName(), event.getPlayer().getUniqueId(), UnpluggedPlayerManager.getInstance().count());
            event.quitMessage(null);
            return;
        }

        final var player = event.getPlayer();
        if (UnpluggedPlayerManager.getInstance().isPending(player.getUniqueId())) {
            event.quitMessage(UnpluggedChatFormatting.formatUnpluggedBroadcast(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        if (!(((CraftPlayer) event.getPlayer()).getHandle() instanceof UnpluggedServerPlayer)) {
            return;
        }

        final var reason = PlainTextComponentSerializer.plainText().serialize(event.reason());

        if (!KICK_REASON_PACKETEVENTS.equals(reason)) {
            logDebug("Let a kick of unplugged player {} ({}) through: {}", event.getPlayer().getName(), event.getPlayer().getUniqueId(), reason);
            return;
        }

        event.setCancelled(true);
        logDebug("Refused a kick of unplugged player {} ({}): {}", event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.reason());
    }

    private void logStartupSummary() {
        final var options = UnpluggedOptions.getInstance();

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

    private String getTargetMinecraftVersion() {
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
