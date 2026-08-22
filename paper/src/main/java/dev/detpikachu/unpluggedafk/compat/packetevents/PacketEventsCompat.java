package dev.detpikachu.unpluggedafk.compat.packetevents;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class PacketEventsCompat {

    private static final String PLUGIN_NAME = "packetevents";

    public static void register(UnpluggedAfk plugin) {
        final var pluginManager = plugin.getServer().getPluginManager();

        if (!pluginManager.isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        pluginManager.registerEvents(new PacketEventsListener(), plugin);
        LOGGER.info("PacketEvents detected. Injecting kick prevention so a bot remains online.");
    }
}
