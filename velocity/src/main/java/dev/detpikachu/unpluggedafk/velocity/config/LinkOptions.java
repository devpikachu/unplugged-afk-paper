package dev.detpikachu.unpluggedafk.velocity.config;

import dev.detpikachu.unpluggedafk.common.network.Handshake;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class LinkOptions extends OptionsBase {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 25580;
    private static final String DEFAULT_SECRET = "";

    private static final String KEY_SECTION = "link";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_SECRET = "secret";

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final int SECRET_BYTES = 32;

    private final String host;
    private final int port;
    private final String secret;

    public LinkOptions() {
        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_SECRET);
    }

    private LinkOptions(String host, int port, String secret) {
        this.host = host;
        this.port = port;
        this.secret = secret;
    }

    public static LinkOptions deserialize(Map<?, ?> values, Logger logger) {
        final var section = values.get(KEY_SECTION) instanceof Map<?, ?> nested ? nested : Map.of();

        return new LinkOptions(
                string(section, KEY_HOST, DEFAULT_HOST),
                port(section, logger),
                string(section, KEY_SECRET, DEFAULT_SECRET));
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getSecret() {
        return this.secret;
    }

    public LinkOptions withGeneratedSecret() {
        return new LinkOptions(this.host, this.port, Handshake.newToken(SECRET_BYTES));
    }

    public Map<String, Object> serialize() {
        final var section = new LinkedHashMap<String, Object>();
        section.put(KEY_HOST, this.host);
        section.put(KEY_PORT, this.port);
        section.put(KEY_SECRET, this.secret);

        return Map.of(KEY_SECTION, section);
    }

    private static int port(Map<?, ?> section, Logger logger) {
        final var port = integer(section, KEY_PORT, DEFAULT_PORT);

        if (port >= MIN_PORT && port <= MAX_PORT) {
            return port;
        }

        logger.warn("link.port of {} is outside 1-65535. Resetting to {}.", port, DEFAULT_PORT);
        return DEFAULT_PORT;
    }
}
