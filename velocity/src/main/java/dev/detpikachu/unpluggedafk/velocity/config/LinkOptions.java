package dev.detpikachu.unpluggedafk.velocity.config;

import de.exlll.configlib.Configuration;
import de.exlll.configlib.PostProcess;
import dev.detpikachu.unpluggedafk.common.config.LinkOptionsBase;
import dev.detpikachu.unpluggedafk.common.network.Handshake;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Configuration
public final class LinkOptions extends LinkOptionsBase {

    private static final int SECRET_BYTES = 32;

    void mintSecret() {
        this.setSecret(Handshake.newToken(SECRET_BYTES));
    }

    @PostProcess
    void normalize() {
        this.resetBlankHost();
        this.clampPort();
    }
}
