package dev.detpikachu.unpluggedAfk;

import dev.detpikachu.unpluggedAfk.commands.UnpluggedCommands;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import dev.detpikachu.unpluggedAfk.formatting.UnpluggedChatFormatting;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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

    public static ComponentLogger LOGGER;

    @Override
    public void onEnable() {
        // Logger
        LOGGER = getComponentLogger();

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
        UnpluggedPlayerManager.getInstance().getPlayers().forEachValue(Long.MAX_VALUE, unpluggedPlayer -> unpluggedPlayer.kill(Component.literal(KILL_REASON_DISABLED)));
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
            event.quitMessage(UnpluggedChatFormatting.formatUnplugged(player));
        }
    }
}
