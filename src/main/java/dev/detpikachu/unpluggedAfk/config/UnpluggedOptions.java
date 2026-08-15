package dev.detpikachu.unpluggedAfk.config;

import org.bukkit.configuration.file.FileConfiguration;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.DEFAULT_DURATION;

public final class UnpluggedOptions {

    private static final UnpluggedOptions INSTANCE = new UnpluggedOptions();

    private static final String DEFAULT_DURATION_MINS = "defaultDurationMins";
    private static final String MAX_DURATION_MINS = "maxDurationMins";

    private int defaultDurationMins = 480;
    private int maxDurationMins = 480;

    public static UnpluggedOptions getInstance() {
        return INSTANCE;
    }

    public int getDefaultDurationMins() {
        // TODO: This is less than ideal since this method has side-effects. Good enough for MVP.
        if (this.defaultDurationMins <= 0) {
            this.defaultDurationMins = DEFAULT_DURATION;
        }

        if (this.defaultDurationMins > this.getMaxDurationMins()) {
            this.defaultDurationMins = this.maxDurationMins;
        }

        return this.defaultDurationMins;
    }

    public int getMaxDurationMins() {
        // TODO: This is less than ideal since this method has side-effects. Good enough for MVP.
        if (this.maxDurationMins <= 0) {
            this.maxDurationMins = DEFAULT_DURATION;
        }

        return this.maxDurationMins;
    }

    public static void deserialize(FileConfiguration config) {
        INSTANCE.defaultDurationMins = config.getInt(DEFAULT_DURATION_MINS, DEFAULT_DURATION);
        INSTANCE.maxDurationMins = config.getInt(MAX_DURATION_MINS, DEFAULT_DURATION);
    }
}
