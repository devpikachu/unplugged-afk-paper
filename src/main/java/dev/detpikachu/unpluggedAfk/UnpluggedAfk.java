package dev.detpikachu.unpluggedAfk;

import dev.detpikachu.unpluggedAfk.commands.UnpluggedCommands;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import dev.detpikachu.unpluggedAfk.formatting.UnpluggedChatFormatting;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class UnpluggedAfk extends JavaPlugin implements Listener {

    public static ComponentLogger LOGGER;

    @Override
    public void onEnable() {
        // Logger
        LOGGER = getComponentLogger();

        // Config
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(UnpluggedOptions.class);
        UnpluggedOptions.deserialize(getConfig().getValues(true));

        // Events
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            UnpluggedCommands.register(commands.registrar());
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (((CraftPlayer) event.getPlayer()).getHandle() instanceof UnpluggedServerPlayer) {
            event.joinMessage(null);
            return;
        }

        final var player = event.getPlayer();
        if (UnpluggedPlayerManager.getInstance().isUnplugged(player.getUniqueId())) {
            event.joinMessage(UnpluggedChatFormatting.formatReplugged(player));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (((CraftPlayer) event.getPlayer()).getHandle() instanceof UnpluggedServerPlayer) {
            event.quitMessage(null);
            return;
        }

        final var player = event.getPlayer();
        if (UnpluggedPlayerManager.getInstance().isPending(player.getUniqueId())) {
            event.quitMessage(UnpluggedChatFormatting.formatUnplugged(player));
        }
    }
}
