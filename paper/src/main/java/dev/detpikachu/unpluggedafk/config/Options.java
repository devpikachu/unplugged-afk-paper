package dev.detpikachu.unpluggedafk.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Options extends OptionsBase {

    private static final Options INSTANCE = new Options();

    private static final boolean DEFAULT_DEBUG = false;
    private static final int DEFAULT_MAX_UNPLUGGED_PLAYERS = 16;
    private static final int DEFAULT_MAX_DURATION_MINS = 480;

    private static final String KEY_DEBUG = "debug";
    private static final String KEY_MAX_UNPLUGGED_PLAYERS = "maxUnpluggedPlayers";
    private static final String KEY_MAX_DURATION_MINS = "maxDurationMins";

    private boolean isDebug = DEFAULT_DEBUG;
    private int maxUnpluggedPlayers = DEFAULT_MAX_UNPLUGGED_PLAYERS;
    private int maxDurationMins = DEFAULT_MAX_DURATION_MINS;

    private LinkOptions link = new LinkOptions();

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
        INSTANCE.isDebug = config.getBoolean(KEY_DEBUG, DEFAULT_DEBUG);
        INSTANCE.maxUnpluggedPlayers = atLeastOne(config, KEY_MAX_UNPLUGGED_PLAYERS, DEFAULT_MAX_UNPLUGGED_PLAYERS);
        INSTANCE.maxDurationMins = atLeastOne(config, KEY_MAX_DURATION_MINS, DEFAULT_MAX_DURATION_MINS);
        INSTANCE.link.deserialize(config);
    }
}
