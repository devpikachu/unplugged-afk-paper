package dev.detpikachu.unpluggedafk.velocity.session;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class SessionStore {

    private final Logger logger;
    private final ConcurrentHashMap<UUID, Session> sessions;

    public SessionStore(Logger logger) {
        this.logger = logger;
        this.sessions = new ConcurrentHashMap<>();
    }

    public boolean isAlive(UUID uuid) {
        final var session = this.sessions.get(uuid);
        return session != null && session.isAlive();
    }

    public boolean isHeldBy(UUID uuid, String serverName) {
        final var session = this.sessions.get(uuid);
        return session != null && session.isAlive() && session.serverName().equals(serverName);
    }

    public boolean start(
            String serverName, UUID uuid, String username, Session.@Nullable Skin skin, long secondsRemaining) {
        this.pruneExpired();

        final var previous = this.sessions.get(uuid);

        if (previous != null && previous.isAlive() && !previous.serverName().equals(serverName)) {
            this.logger.warn(
                    "Refused a session for {} ({}) on {}: they already have a live session on {}.",
                    username,
                    uuid,
                    serverName,
                    previous.serverName());
            return false;
        }

        this.sessions.put(
                uuid, new Session(serverName, username, skin, Instant.now().plusSeconds(secondsRemaining)));
        return true;
    }

    public void end(String serverName, UUID uuid) {
        this.sessions.computeIfPresent(uuid, (key, session) -> {
            if (!session.serverName().equals(serverName)) {
                this.logger.warn(
                        "Ignored a SESSION_END for {} from {}: the session is held by {}.",
                        uuid,
                        serverName,
                        session.serverName());
                return session;
            }

            return session.ended();
        });
        this.pruneExpired();
    }

    public void replace(String serverName, Map<UUID, Session> incoming) {
        this.dropServer(serverName);

        incoming.forEach((uuid, session) -> {
            final var previous = this.sessions.get(uuid);

            if (previous != null && previous.isAlive() && !previous.serverName().equals(serverName)) {
                this.logger.warn(
                        "Ignored a synced session for {} from {}: they have a live session on {}.",
                        uuid,
                        serverName,
                        previous.serverName());
                return;
            }

            this.sessions.put(uuid, session);
        });

        this.pruneExpired();
    }

    public void dropServer(String serverName) {
        this.sessions.values().removeIf(session -> session.serverName().equals(serverName));
    }

    public Optional<Session> consume(UUID uuid) {
        final var session = this.sessions.remove(uuid);

        if (session == null) {
            return Optional.empty();
        }

        if (!session.canRoute()) {
            this.logger.info(
                    "The session for {} on {} expired at {}, so they fall back to the try list.",
                    uuid,
                    session.serverName(),
                    session.expiresAt());
            return Optional.empty();
        }

        return Optional.of(session);
    }

    private void pruneExpired() {
        this.sessions.values().removeIf(session -> !session.canRoute());
    }
}
