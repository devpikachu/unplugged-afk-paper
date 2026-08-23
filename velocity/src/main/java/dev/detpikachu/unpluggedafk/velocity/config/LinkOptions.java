package dev.detpikachu.unpluggedafk.velocity.config;

import dev.detpikachu.unpluggedafk.common.Handshake;
import dev.detpikachu.unpluggedafk.velocity.Constants.Defaults;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public record LinkOptions(String host, int port, String secret) {

    private static final String KEY_SECTION = "link";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_SECRET = "secret";

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    public static LinkOptions deserialize(Map<?, ?> values, Logger logger) {
        final var section = values.get(KEY_SECTION) instanceof Map<?, ?> nested ? nested : Map.of();

        return new LinkOptions(
                string(section, KEY_HOST, Defaults.LINK_HOST),
                port(section, logger),
                string(section, KEY_SECRET, Defaults.LINK_SECRET));
    }

    public LinkOptions withGeneratedSecret() {
        return new LinkOptions(this.host, this.port, Handshake.newToken(Defaults.LINK_SECRET_BYTES));
    }

    public Map<String, Object> serialize() {
        final var section = new LinkedHashMap<String, Object>();
        section.put(KEY_HOST, this.host);
        section.put(KEY_PORT, this.port);
        section.put(KEY_SECRET, this.secret);

        return Map.of(KEY_SECTION, section);
    }

    private static String string(Map<?, ?> section, String key, String fallback) {
        final var value = section.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private static int port(Map<?, ?> section, Logger logger) {
        final var value = section.get(KEY_PORT);
        final var port = value instanceof Number number ? number.intValue() : Defaults.LINK_PORT;

        if (port >= MIN_PORT && port <= MAX_PORT) {
            return port;
        }

        logger.warn("link.port of {} is outside 1-65535. Resetting to {}.", port, Defaults.LINK_PORT);
        return Defaults.LINK_PORT;
    }
}
