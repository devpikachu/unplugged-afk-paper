package dev.detpikachu.unpluggedAfk.config;

import dev.detpikachu.unpluggedAfk.UnpluggedConstants;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

public final class UnpluggedOptions implements ConfigurationSerializable {

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
            this.defaultDurationMins = UnpluggedConstants.DEFAULT_DURATION;
        }

        if (this.defaultDurationMins > this.getMaxDurationMins()) {
            this.defaultDurationMins = this.maxDurationMins;
        }

        return this.defaultDurationMins;
    }

    public int getMaxDurationMins() {
        // TODO: This is less than ideal since this method has side-effects. Good enough for MVP.
        if (this.maxDurationMins <= 0) {
            this.maxDurationMins = UnpluggedConstants.DEFAULT_DURATION;
        }

        return this.maxDurationMins;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        var data = new HashMap<String, Object>();

        data.put(DEFAULT_DURATION_MINS, this.defaultDurationMins);
        data.put(MAX_DURATION_MINS, this.maxDurationMins);

        return data;
    }

    public static void deserialize(Map<String, Object> data) {
        Object holder;

        if ((holder = data.get(DEFAULT_DURATION_MINS)) != null) {
            INSTANCE.defaultDurationMins = (int) holder;
        }

        if ((holder = data.get(MAX_DURATION_MINS)) != null) {
            INSTANCE.maxDurationMins = (int) holder;
        }
    }
}
