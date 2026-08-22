package dev.detpikachu.unpluggedafk.velocity;

import java.time.Instant;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record UnpluggedSession(String serverName, Instant expiresAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
