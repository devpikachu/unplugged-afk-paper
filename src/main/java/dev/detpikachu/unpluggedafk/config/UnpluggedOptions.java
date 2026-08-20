package dev.detpikachu.unpluggedafk.config;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import org.bukkit.configuration.file.FileConfiguration;

import static dev.detpikachu.unpluggedafk.UnpluggedConstants.DEFAULT_DEBUG;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.DEFAULT_MAX_DURATION_MINS;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.DEFAULT_MAX_UNPLUGGED_PLAYERS;

public final class UnpluggedOptions {

    private static final UnpluggedOptions INSTANCE = new UnpluggedOptions();

    private static final String KEY_DEBUG = "debug";
    private static final String KEY_MAX_UNPLUGGED_PLAYERS = "maxUnpluggedPlayers";
    private static final String KEY_MAX_DURATION_MINS = "maxDurationMins";

    private boolean isDebug = DEFAULT_DEBUG;
    private int maxUnpluggedPlayers = DEFAULT_MAX_UNPLUGGED_PLAYERS;
    private int maxDurationMins = DEFAULT_MAX_DURATION_MINS;

    public static UnpluggedOptions getInstance() {
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
        INSTANCE.isDebug = config.getBoolean(KEY_DEBUG, DEFAULT_DEBUG);
        INSTANCE.maxUnpluggedPlayers = config.getInt(KEY_MAX_UNPLUGGED_PLAYERS, DEFAULT_MAX_UNPLUGGED_PLAYERS);
        INSTANCE.maxDurationMins = config.getInt(KEY_MAX_DURATION_MINS, DEFAULT_MAX_DURATION_MINS);

        if (INSTANCE.maxUnpluggedPlayers < 1) {
            UnpluggedAfk.LOGGER.warn("Max unplugged players {} is invalid. The value must be greater than or equal to 1. Resetting to {}.", INSTANCE.maxUnpluggedPlayers, DEFAULT_MAX_UNPLUGGED_PLAYERS);
            INSTANCE.maxUnpluggedPlayers = DEFAULT_MAX_UNPLUGGED_PLAYERS;
        }

        if (INSTANCE.maxDurationMins < 1) {
            UnpluggedAfk.LOGGER.warn("Max duration of {} is invalid. The value must be greater than or equal to 1. Resetting to {}.", INSTANCE.maxDurationMins, DEFAULT_MAX_DURATION_MINS);
            INSTANCE.maxDurationMins = DEFAULT_MAX_DURATION_MINS;
        }
    }
}
