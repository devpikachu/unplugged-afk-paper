package dev.detpikachu.unpluggedafk.velocity;

import java.time.Instant;

public record UnpluggedSession(String serverName, Instant expiresAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
