package dev.detpikachu.unpluggedAfk.config;

import dev.detpikachu.unpluggedAfk.UnpluggedAfk;
import org.bukkit.configuration.file.FileConfiguration;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.DEFAULT_MAX_DURATION_MINS;
import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.DEFAULT_MAX_UNPLUGGED_PLAYERS;

public final class UnpluggedOptions {

    private static final UnpluggedOptions INSTANCE = new UnpluggedOptions();

    private static final String KEY_MAX_UNPLUGGED_PLAYERS = "maxUnpluggedPlayers";
    private static final String KEY_MAX_DURATION_MINS = "maxDurationMins";

    private int maxUnpluggedPlayers = DEFAULT_MAX_UNPLUGGED_PLAYERS;
    private int maxDurationMins = DEFAULT_MAX_DURATION_MINS;

    public static UnpluggedOptions getInstance() {
        return INSTANCE;
    }

    public int getMaxUnpluggedPlayers() {
        return this.maxUnpluggedPlayers;
    }

    public int getMaxDurationMins() {
        return this.maxDurationMins;
    }

    public static void deserialize(FileConfiguration config) {
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
