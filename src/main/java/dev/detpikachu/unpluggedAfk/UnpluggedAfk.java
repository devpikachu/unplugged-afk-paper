package dev.detpikachu.unpluggedAfk;

import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class UnpluggedAfk extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Config
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(UnpluggedOptions.class);
        UnpluggedOptions.deserialize(getConfig().getValues(true));

        // Events
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(UnpluggedCommands.unplug());
            commands.registrar().register(UnpluggedCommands.unplugged());
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }
}
