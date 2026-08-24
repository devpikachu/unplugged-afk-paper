package dev.detpikachu.unpluggedafk.velocity.session;

import dev.detpikachu.unpluggedafk.common.network.Protocol;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@ApiStatus.Internal
public record Session(
        String serverName,
        @Nullable String username,
        @Nullable Skin skin,
        Instant expiresAt) {

    public boolean isAlive() {
        return Instant.now().isBefore(this.expiresAt);
    }

    public boolean canRoute() {
        return Instant.now().isBefore(this.expiresAt.plusSeconds(Protocol.GRACE_SECS));
    }

    public Session ended() {
        return new Session(this.serverName, this.username, this.skin, Instant.now());
    }

    public record Skin(String value, @Nullable String signature) {}
}
