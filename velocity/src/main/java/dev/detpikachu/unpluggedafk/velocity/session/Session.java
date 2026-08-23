package dev.detpikachu.unpluggedafk.velocity.session;

import dev.detpikachu.unpluggedafk.velocity.Constants.Sessions;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

@ApiStatus.Internal
public record Session(String serverName, @Nullable String username, @Nullable Skin skin, Instant expiresAt) {

    public boolean isAlive() {
        return Instant.now().isBefore(this.expiresAt);
    }

    public boolean canRoute() {
        return Instant.now().isBefore(this.expiresAt.plus(Duration.ofMinutes(Sessions.GRACE_MINS)));
    }

    public record Skin(String value, @Nullable String signature) {}
}
