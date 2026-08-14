package dev.detpikachu.unpluggedAfk.config;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

public final class UnpluggedOptions implements ConfigurationSerializable {

    private static final UnpluggedOptions INSTANCE = new UnpluggedOptions();

    private boolean isDebug;

    public static UnpluggedOptions getInstance() {
        return INSTANCE;
    }

    public boolean getIsDebug() {
        return isDebug;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        var data = new HashMap<String, Object>();

        data.put("debug", this.isDebug);

        return data;
    }

    public static void deserialize(Map<String, Object> data) {
        INSTANCE.isDebug = (boolean) data.get("debug");
    }
}
