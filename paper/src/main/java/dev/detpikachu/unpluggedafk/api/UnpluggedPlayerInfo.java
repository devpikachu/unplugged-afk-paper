package dev.detpikachu.unpluggedafk.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable snapshot of one unplugged player, taken at the moment the API call returned.
 *
 * <p>Membership is not live: the bot described here may since have expired, died, or been reaped. {@link #remaining()}
 * is recomputed against the current clock on every call, so it stays accurate for as long as this record is held, but
 * the record's existence is no guarantee the bot still stands.
 *
 * @param uuid         the UUID of the player who unplugged, which their bot shares with them
 * @param name         the player's name
 * @param durationMins the window the player asked for, in minutes
 * @param reason       the reason the player gave to {@code /unplug}
 * @param startedAt    when the bot was created
 * @param expiresAt    when the bot is due to be reaped, being {@code startedAt} plus {@code durationMins}
 * @param isFake       whether this is a throwaway bot from {@code /unplugged debug spawn-fake} rather than a real
 *                     player's session
 */
public record UnpluggedPlayerInfo(
        UUID uuid,
        String name,
        int durationMins,
        String reason,
        Instant startedAt,
        Instant expiresAt,
        boolean isFake) {

    /**
     * Rejects a null reference component, so a consumer that builds one by hand fails here rather than inside an
     * unrelated listener later.
     *
     * @throws NullPointerException if any of the reference components is {@code null}
     */
    public UnpluggedPlayerInfo {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /**
     * How long is left before this bot is due to be reaped, recomputed against the current clock on every call.
     *
     * @return the time remaining, or {@link Duration#ZERO} if the bot is already past its expiry and awaiting reaping
     */
    public Duration remaining() {
        final var remaining = Duration.between(Instant.now(), this.expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
