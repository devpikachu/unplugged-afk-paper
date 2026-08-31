package dev.detpikachu.unpluggedafk.config;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class OptionsBase {

    protected static int atLeastOne(FileConfiguration config, String key, int defaultValue) {
        final var value = config.getInt(key, defaultValue);

        if (value >= 1) {
            return value;
        }

        Log.warn(
                "{} of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.",
                key,
                value,
                defaultValue);
        return defaultValue;
    }

    protected static int inRange(FileConfiguration config, String key, int defaultValue, int min, int max) {
        final var value = config.getInt(key, defaultValue);

        if (value >= min && value <= max) {
            return value;
        }

        Log.warn(
                "{} of {} is invalid. The value must be between {} and {}. Resetting to {}.",
                key,
                value,
                min,
                max,
                defaultValue);
        return defaultValue;
    }
}
