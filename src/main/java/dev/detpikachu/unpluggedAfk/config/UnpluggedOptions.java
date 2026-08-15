package dev.detpikachu.unpluggedAfk.config;

import dev.detpikachu.unpluggedAfk.UnpluggedConstants;
import org.bukkit.configuration.file.FileConfiguration;

public final class UnpluggedOptions {

    private static final UnpluggedOptions INSTANCE = new UnpluggedOptions();

    private static final String DEFAULT_DURATION_MINS = "defaultDurationMins";
    private static final String MAX_DURATION_MINS = "maxDurationMins";

    private int defaultDurationMins = UnpluggedConstants.DEFAULT_DURATION_MINS;
    private int maxDurationMins = UnpluggedConstants.DEFAULT_DURATION_MINS;

    public static UnpluggedOptions getInstance() {
        return INSTANCE;
    }

    public int getDefaultDurationMins() {
        // TODO: This is less than ideal since this method has side-effects. Good enough for MVP.
        if (this.defaultDurationMins <= 0) {
            this.defaultDurationMins = UnpluggedConstants.DEFAULT_DURATION_MINS;
        }

        if (this.defaultDurationMins > this.getMaxDurationMins()) {
            this.defaultDurationMins = this.maxDurationMins;
        }

        return this.defaultDurationMins;
    }

    public int getMaxDurationMins() {
        // TODO: This is less than ideal since this method has side-effects. Good enough for MVP.
        if (this.maxDurationMins <= 0) {
            this.maxDurationMins = UnpluggedConstants.DEFAULT_DURATION_MINS;
        }

        return this.maxDurationMins;
    }

    public static void deserialize(FileConfiguration config) {
        INSTANCE.defaultDurationMins = config.getInt(DEFAULT_DURATION_MINS, UnpluggedConstants.DEFAULT_DURATION_MINS);
        INSTANCE.maxDurationMins = config.getInt(MAX_DURATION_MINS, UnpluggedConstants.DEFAULT_DURATION_MINS);
    }
}
