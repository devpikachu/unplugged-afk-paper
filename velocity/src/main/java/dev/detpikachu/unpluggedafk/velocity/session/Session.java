package dev.detpikachu.unpluggedafk.velocity.session;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

@ApiStatus.Internal
public record Session(
        String serverName,
        @Nullable String username,
        @Nullable Skin skin,
        Instant expiresAt) {

    private static final int GRACE_MINS = 5;

    public boolean isAlive() {
        return Instant.now().isBefore(this.expiresAt);
    }

    public boolean canRoute() {
        return Instant.now().isBefore(this.expiresAt.plus(Duration.ofMinutes(GRACE_MINS)));
    }

    public record Skin(String value, @Nullable String signature) {}
}
