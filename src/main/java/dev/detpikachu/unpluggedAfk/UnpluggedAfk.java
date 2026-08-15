package dev.detpikachu.unpluggedAfk;

import dev.detpikachu.unpluggedAfk.commands.UnpluggedCommands;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import dev.detpikachu.unpluggedAfk.formatting.UnpluggedChatFormatting;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.util.Properties;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.network.chat.Component;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.KILL_REASON_DISABLED;

public final class UnpluggedAfk extends JavaPlugin implements Listener {

    private static final String PROPERTIES_RESOURCE = "unplugged-afk.properties";
    private static final String KEY_MINECRAFT_VERSION = "minecraftVersion";

    public static ComponentLogger LOGGER;

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
    }

    @Override
    public void onDisable() {
        UnpluggedPlayerManager.getInstance().forEach(unpluggedPlayer -> unpluggedPlayer.kill(Component.literal(KILL_REASON_DISABLED)));
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
            event.quitMessage(null);
            return;
        }

        final var player = event.getPlayer();
        if (UnpluggedPlayerManager.getInstance().isPending(player.getUniqueId())) {
            event.quitMessage(UnpluggedChatFormatting.formatUnpluggedBroadcast(player));
        }
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
