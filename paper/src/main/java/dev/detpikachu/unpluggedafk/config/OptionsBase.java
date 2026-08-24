package dev.detpikachu.unpluggedafk.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public abstract class OptionsBase {

    protected static int atLeastOne(FileConfiguration config, String key, int defaultValue) {
        final var value = config.getInt(key, defaultValue);

        if (value >= 1) {
            return value;
        }

        LOGGER.warn(
                "{} of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.",
                key,
                value,
                defaultValue);
        return defaultValue;
    }
}
