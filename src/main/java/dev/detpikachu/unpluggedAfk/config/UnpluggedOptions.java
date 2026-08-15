package dev.detpikachu.unpluggedAfk.config;

import dev.detpikachu.unpluggedAfk.UnpluggedAfk;
import org.bukkit.configuration.file.FileConfiguration;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.DEFAULT_MAX_DURATION_MINS;

public final class UnpluggedOptions {

    private static final UnpluggedOptions INSTANCE = new UnpluggedOptions();

    private static final String KEY_MAX_DURATION_MINS = "maxDurationMins";

    private int maxDurationMins = DEFAULT_MAX_DURATION_MINS;

    public static UnpluggedOptions getInstance() {
        return INSTANCE;
    }

    public int getMaxDurationMins() {
        return this.maxDurationMins;
    }

    public static void deserialize(FileConfiguration config) {
        INSTANCE.maxDurationMins = config.getInt(KEY_MAX_DURATION_MINS, DEFAULT_MAX_DURATION_MINS);

        if (INSTANCE.maxDurationMins < 1) {
            UnpluggedAfk.LOGGER.warn("Max duration of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.", INSTANCE.maxDurationMins, DEFAULT_MAX_DURATION_MINS);
            INSTANCE.maxDurationMins = DEFAULT_MAX_DURATION_MINS;
        }
    }
}
