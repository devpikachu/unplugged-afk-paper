package dev.detpikachu.unpluggedafk.compat.husksync;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class HuskSyncCompat {

    private static final String PLUGIN_NAME = "HuskSync";

    public static void register(UnpluggedAfk plugin) {
        final var pluginManager = plugin.getServer().getPluginManager();

        if (!pluginManager.isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        pluginManager.registerEvents(new HuskSyncListener(), plugin);
        LOGGER.info(
                "HuskSync detected. Injecting data sync fixing mechanism so a bot keeps the inventory it inherits.");
    }
}
