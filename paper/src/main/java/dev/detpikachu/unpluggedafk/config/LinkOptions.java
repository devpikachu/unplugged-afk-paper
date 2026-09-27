package dev.detpikachu.unpluggedafk.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.PostProcess;
import dev.detpikachu.unpluggedafk.common.config.LinkOptionsBase;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Configuration
public final class LinkOptions extends LinkOptionsBase {

    @Comment("This backend's name, exactly as velocity.toml spells it.")
    private String serverName = "";

    public String getServerName() {
        return this.serverName;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && !this.serverName.isBlank();
    }

    @PostProcess
    void clamp() {
        this.clampPort();
    }
}
