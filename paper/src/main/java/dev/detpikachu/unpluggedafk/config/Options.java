package dev.detpikachu.unpluggedafk.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.PostProcess;
import dev.detpikachu.unpluggedafk.common.config.Clamps;
import dev.detpikachu.unpluggedafk.common.config.MessageOptions;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Configuration
public final class Options {

    static final int CURRENT_VERSION = 1;

    private static final int DEFAULT_MAX_UNPLUGGED_PLAYERS = 16;
    private static final int DEFAULT_MAX_DURATION_MINS = 480;

    @Comment("The shape of this file, which the plugin upgrades on its own. Do not edit it.")
    private int version = CURRENT_VERSION;

    @Comment("Enables the /unplugged debug commands and a dump file for every unplug.")
    private boolean debug = false;

    @Comment("How many unplugged players may exist at the same time, at least 1.")
    private int maxUnpluggedPlayers = DEFAULT_MAX_UNPLUGGED_PLAYERS;

    @Comment("The longest a player may unplug for, in minutes, at least 1.")
    private int maxDurationMins = DEFAULT_MAX_DURATION_MINS;

    @Comment("The link to the Unplugged AFK companion on the proxy. Only used behind a Velocity proxy.")
    private LinkOptions link = new LinkOptions();

    @Comment("Text the plugin shows to players.")
    private MessageOptions messages = new MessageOptions();

    public int getVersion() {
        return this.version;
    }

    public boolean isDebug() {
        return this.debug;
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

    public MessageOptions getMessages() {
        return this.messages;
    }

    @PostProcess
    void clamp() {
        this.maxUnpluggedPlayers =
                Clamps.atLeastOne("maxUnpluggedPlayers", this.maxUnpluggedPlayers, DEFAULT_MAX_UNPLUGGED_PLAYERS);
        this.maxDurationMins = Clamps.atLeastOne("maxDurationMins", this.maxDurationMins, DEFAULT_MAX_DURATION_MINS);
    }
}
