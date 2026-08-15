package dev.detpikachu.unpluggedAfk.config;

import org.bukkit.configuration.file.FileConfiguration;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.DEFAULT_DURATION_MINS;

public final class UnpluggedOptions {

    private static final UnpluggedOptions INSTANCE = new UnpluggedOptions();

    private static final String KEY_DEFAULT_DURATION_MINS = "defaultDurationMins";
    private static final String KEY_MAX_DURATION_MINS = "maxDurationMins";

    private int defaultDurationMins = DEFAULT_DURATION_MINS;
    private int maxDurationMins = DEFAULT_DURATION_MINS;

    public static UnpluggedOptions getInstance() {
        return INSTANCE;
    }

    public int getDefaultDurationMins() {
        // TODO: This is less than ideal since this method has side-effects. Good enough for MVP.
        if (this.defaultDurationMins <= 0) {
            this.defaultDurationMins = DEFAULT_DURATION_MINS;
        }

        if (this.defaultDurationMins > this.getMaxDurationMins()) {
            this.defaultDurationMins = this.maxDurationMins;
        }

        return this.defaultDurationMins;
    }

    public int getMaxDurationMins() {
        // TODO: This is less than ideal since this method has side-effects. Good enough for MVP.
        if (this.maxDurationMins <= 0) {
            this.maxDurationMins = DEFAULT_DURATION_MINS;
        }

        return this.maxDurationMins;
    }

    public static void deserialize(FileConfiguration config) {
        INSTANCE.defaultDurationMins = config.getInt(KEY_DEFAULT_DURATION_MINS, DEFAULT_DURATION_MINS);
        INSTANCE.maxDurationMins = config.getInt(KEY_MAX_DURATION_MINS, DEFAULT_DURATION_MINS);
    }
}
