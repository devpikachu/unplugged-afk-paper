package dev.detpikachu.unpluggedafk.velocity.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.detpikachu.unpluggedafk.common.config.MessageOptions;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Configuration
public final class Options {

    static final int CURRENT_VERSION = 1;

    @Comment("The shape of this file, which the plugin upgrades on its own. Do not edit it.")
    private int version = CURRENT_VERSION;

    @Comment("Logs link, presence and relay activity to the console.")
    private boolean debug = false;

    @Comment("The link every backend dials to report its unplugged players.")
    private LinkOptions link = new LinkOptions();

    @Comment("Text the plugin shows to players.")
    private MessageOptions messages = new MessageOptions();

    public int getVersion() {
        return this.version;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public LinkOptions getLink() {
        return this.link;
    }

    public MessageOptions getMessages() {
        return this.messages;
    }
}
