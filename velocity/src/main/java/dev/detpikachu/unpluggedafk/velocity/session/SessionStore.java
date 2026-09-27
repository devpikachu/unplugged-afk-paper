package dev.detpikachu.unpluggedafk.velocity.session;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class SessionStore {

    private final ConcurrentHashMap<UUID, Session> sessions;

    public SessionStore() {
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

    public boolean isHeldElsewhere(UUID uuid, String serverName) {
        final var session = this.sessions.get(uuid);
        return session != null && session.isAlive() && !session.serverName().equals(serverName);
    }

    public @Nullable Session find(UUID uuid) {
        final var session = this.sessions.get(uuid);
        return session != null && session.isAlive() ? session : null;
    }

    public int count() {
        return (int) this.sessions.values().stream().filter(Session::isAlive).count();
    }

    public boolean start(UUID uuid, Session session) {
        this.pruneExpired();

        final var previous = this.sessions.get(uuid);

        if (previous != null && previous.isAlive() && !previous.serverName().equals(session.serverName())) {
            Log.warn(
                    "Refused a session for {} ({}) on {}: they already have a live session on {}.",
                    session.username(),
                    uuid,
                    session.serverName(),
                    previous.serverName());
            return false;
        }

        this.sessions.put(uuid, session);
        return true;
    }

    public void end(String serverName, UUID uuid) {
        this.sessions.computeIfPresent(uuid, (key, session) -> {
            if (!session.serverName().equals(serverName)) {
                Log.warn(
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

    public Set<UUID> replace(String serverName, Map<UUID, Session> incoming) {
        this.dropServer(serverName);

        final var accepted = new HashSet<UUID>();

        incoming.forEach((uuid, session) -> {
            final var previous = this.sessions.get(uuid);

            if (previous != null && previous.isAlive() && !previous.serverName().equals(serverName)) {
                Log.warn(
                        "Ignored a synced session for {} from {}: they have a live session on {}.",
                        uuid,
                        serverName,
                        previous.serverName());
                return;
            }

            this.sessions.put(uuid, session);
            accepted.add(uuid);
        });

        this.pruneExpired();

        return accepted;
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
            Log.info(
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
