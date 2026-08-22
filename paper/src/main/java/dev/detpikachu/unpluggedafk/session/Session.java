package dev.detpikachu.unpluggedafk.session;

import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.time.Instant;

@ApiStatus.Internal
public record Session(int durationMins, String reason, Instant startedAt, Instant expiresAt, boolean isFake) {

    public Session(int durationMins, String reason, boolean isFake) {
        this(durationMins, reason, Instant.now(), isFake);
    }

    private Session(int durationMins, String reason, Instant startedAt, boolean isFake) {
        this(durationMins, reason, startedAt, startedAt.plus(Duration.ofMinutes(durationMins)), isFake);
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(this.expiresAt);
    }

    public Duration elapsed() {
        return Duration.between(this.startedAt, Instant.now());
    }

    public Duration remaining() {
        final var remaining = Duration.between(Instant.now(), this.expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
