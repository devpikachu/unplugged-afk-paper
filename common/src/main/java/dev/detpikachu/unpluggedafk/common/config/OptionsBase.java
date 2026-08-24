package dev.detpikachu.unpluggedafk.common.config;

import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.Internal
public abstract class OptionsBase {

    protected static int integer(Map<?, ?> section, String key, int defaultValue) {
        final var value = section.get(key);
        return value instanceof Integer integer ? integer : defaultValue;
    }

    protected static String string(Map<?, ?> section, String key, String defaultValue) {
        final var value = section.get(key);
        return value instanceof String text && !text.isBlank() ? text : defaultValue;
    }
}
