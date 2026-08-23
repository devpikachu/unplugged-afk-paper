package dev.detpikachu.unpluggedafk.config;

import dev.detpikachu.unpluggedafk.Constants.Defaults;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class Options {

    private static final Options INSTANCE = new Options();

    private static final String KEY_DEBUG = "debug";
    private static final String KEY_MAX_UNPLUGGED_PLAYERS = "maxUnpluggedPlayers";
    private static final String KEY_MAX_DURATION_MINS = "maxDurationMins";
    private static final String KEY_LINK_HOST = "link.host";
    private static final String KEY_LINK_PORT = "link.port";
    private static final String KEY_LINK_SECRET = "link.secret";
    private static final String KEY_LINK_SERVER_NAME = "link.serverName";

    private boolean isDebug = Defaults.DEBUG;
    private int maxUnpluggedPlayers = Defaults.MAX_UNPLUGGED_PLAYERS;
    private int maxDurationMins = Defaults.MAX_DURATION_MINS;

    private LinkOptions link = new LinkOptions(
            Defaults.LINK_HOST,
            Defaults.LINK_PORT,
            Defaults.LINK_SECRET,
            Defaults.LINK_SERVER_NAME);

    public static Options getInstance() {
        return INSTANCE;
    }

    public boolean isDebug() {
        return this.isDebug;
    }

    public int getMaxUnpluggedPlayers() {
        return this.maxUnpluggedPlayers;
    }

    public int getMaxDurationMins() {
        return this.maxDurationMins;
    }

    public LinkOptions getLink() {
        return this.link;
    }

    public static void deserialize(FileConfiguration config) {
        INSTANCE.isDebug = config.getBoolean(KEY_DEBUG, Defaults.DEBUG);
        INSTANCE.maxUnpluggedPlayers = atLeastOne(config, KEY_MAX_UNPLUGGED_PLAYERS, Defaults.MAX_UNPLUGGED_PLAYERS);
        INSTANCE.maxDurationMins = atLeastOne(config, KEY_MAX_DURATION_MINS, Defaults.MAX_DURATION_MINS);
        INSTANCE.link = new LinkOptions(
                config.getString(KEY_LINK_HOST, Defaults.LINK_HOST),
                config.getInt(KEY_LINK_PORT, Defaults.LINK_PORT),
                config.getString(KEY_LINK_SECRET, Defaults.LINK_SECRET),
                config.getString(KEY_LINK_SERVER_NAME, Defaults.LINK_SERVER_NAME));
    }

    private static int atLeastOne(FileConfiguration config, String key, int fallback) {
        final var value = config.getInt(key, fallback);

        if (value >= 1) {
            return value;
        }

        LOGGER.warn(
                "{} of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.",
                key,
                value,
                fallback);
        return fallback;
    }
}
