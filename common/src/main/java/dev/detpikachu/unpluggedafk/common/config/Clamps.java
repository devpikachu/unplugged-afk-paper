package dev.detpikachu.unpluggedafk.common.config;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Clamps {

    public static int atLeastOne(String key, int value, int fallback) {
        if (value >= 1) {
            return value;
        }

        Log.warn(
                "{} of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.",
                key,
                value,
                fallback);
        return fallback;
    }

    public static int inRange(String key, int value, int fallback, int min, int max) {
        if (value >= min && value <= max) {
            return value;
        }

        Log.warn(
                "{} of {} is invalid. The value must be between {} and {}. Resetting to {}.",
                key,
                value,
                min,
                max,
                fallback);
        return fallback;
    }
}
