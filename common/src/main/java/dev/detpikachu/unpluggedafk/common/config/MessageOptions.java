package dev.detpikachu.unpluggedafk.common.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Configuration
public final class MessageOptions {

    @Comment("What the label placeholder shows next to an unplugged player. Leave it empty to show nothing.")
    private String label = "🔌";

    public String getLabel() {
        return this.label;
    }
}
