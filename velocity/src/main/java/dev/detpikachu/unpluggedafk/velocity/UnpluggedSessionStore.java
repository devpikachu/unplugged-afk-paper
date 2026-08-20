package dev.detpikachu.unpluggedafk.velocity;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UnpluggedSessionStore {

    private final ConcurrentHashMap<UUID, UnpluggedSession> sessions = new ConcurrentHashMap<>();

    public void start(UUID uuid, String serverName, int durationMins) {
        final var expiresAt = Instant.now().plus(Duration.ofMinutes(durationMins));
        this.sessions.put(uuid, new UnpluggedSession(serverName, expiresAt));
    }

    public Optional<UnpluggedSession> consume(UUID uuid) {
        return Optional.ofNullable(this.sessions.remove(uuid)).filter(session -> !session.isExpired());
    }
}
