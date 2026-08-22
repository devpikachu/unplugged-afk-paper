package dev.detpikachu.unpluggedafk.velocity.session;

import org.jetbrains.annotations.ApiStatus;

import java.time.Instant;

@ApiStatus.Internal
public record Session(String serverName, Instant expiresAt) {

    public boolean isExpired() {
        return !Instant.now().isBefore(this.expiresAt);
    }
}
