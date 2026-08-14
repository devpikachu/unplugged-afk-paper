package dev.detpikachu.unpluggedAfk.config;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

public final class UnpluggedAfkOptions implements ConfigurationSerializable {

    private static UnpluggedAfkOptions instance;

    private boolean isDebug;

    public static UnpluggedAfkOptions getInstance() {
        return instance;
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
        var options = new UnpluggedAfkOptions();

        options.isDebug = (boolean) data.get("debug");

        instance = options;
    }
}
