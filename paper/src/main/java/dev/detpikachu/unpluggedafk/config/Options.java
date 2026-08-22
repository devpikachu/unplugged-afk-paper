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

    private boolean isDebug = Defaults.DEBUG;
    private int maxUnpluggedPlayers = Defaults.MAX_UNPLUGGED_PLAYERS;
    private int maxDurationMins = Defaults.MAX_DURATION_MINS;

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

    public static void deserialize(FileConfiguration config) {
        INSTANCE.isDebug = config.getBoolean(KEY_DEBUG, Defaults.DEBUG);
        INSTANCE.maxUnpluggedPlayers = config.getInt(KEY_MAX_UNPLUGGED_PLAYERS, Defaults.MAX_UNPLUGGED_PLAYERS);
        INSTANCE.maxDurationMins = config.getInt(KEY_MAX_DURATION_MINS, Defaults.MAX_DURATION_MINS);

        if (INSTANCE.maxUnpluggedPlayers < 1) {
            LOGGER.warn(
                    "{} of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.",
                    KEY_MAX_UNPLUGGED_PLAYERS,
                    INSTANCE.maxUnpluggedPlayers,
                    Defaults.MAX_UNPLUGGED_PLAYERS);
            INSTANCE.maxUnpluggedPlayers = Defaults.MAX_UNPLUGGED_PLAYERS;
        }

        if (INSTANCE.maxDurationMins < 1) {
            LOGGER.warn(
                    "{} of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.",
                    KEY_MAX_DURATION_MINS,
                    INSTANCE.maxDurationMins,
                    Defaults.MAX_DURATION_MINS);
            INSTANCE.maxDurationMins = Defaults.MAX_DURATION_MINS;
        }
    }
}
