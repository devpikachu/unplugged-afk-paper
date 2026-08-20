package dev.detpikachu.unpluggedafk.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

import static dev.detpikachu.unpluggedafk.velocity.UnpluggedConstants.SESSIONS_FILE_NAME;
import static dev.detpikachu.unpluggedafk.velocity.UnpluggedConstants.SESSION_GRACE_MINS;

public final class UnpluggedSessionStore {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (instant, type, context) -> new JsonPrimitive(instant.toEpochMilli()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (json, type, context) -> Instant.ofEpochMilli(json.getAsLong()))
            .setPrettyPrinting()
            .create();

    private static final Type SESSIONS_TYPE = new TypeToken<Map<UUID, UnpluggedSession>>() {
    }.getType();

    private final Path file;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, UnpluggedSession> sessions;

    public UnpluggedSessionStore(Path dataDirectory, Logger logger) {
        this.file = dataDirectory.resolve(SESSIONS_FILE_NAME);
        this.logger = logger;
        this.sessions = new ConcurrentHashMap<>();
    }

    public void start(UUID uuid, String serverName, int durationMins) {
        final var expiresAt = Instant.now().plus(Duration.ofMinutes(durationMins + SESSION_GRACE_MINS));
        final var previous = this.sessions.put(uuid, new UnpluggedSession(serverName, expiresAt));

        if (previous != null && !previous.isExpired()) {
            this.logger.warn("{} already had a live session on {}, now replaced by {}. Both backends may be holding a bot for them.", uuid, previous.serverName(), serverName);
        }

        this.save();
    }

    public Optional<UnpluggedSession> consume(UUID uuid) {
        final var session = this.sessions.remove(uuid);

        if (session == null) {
            return Optional.empty();
        }

        this.save();

        if (session.isExpired()) {
            this.logger.info("The session for {} on {} expired at {}, so they fall back to the try list.", uuid, session.serverName(), session.expiresAt());
            return Optional.empty();
        }

        return Optional.of(session);
    }

    public void load() {
        if (!Files.isRegularFile(this.file)) {
            this.logger.info("No session file at {} yet, starting with no sessions.", this.file);
            return;
        }

        try (final var reader = Files.newBufferedReader(this.file)) {
            final Map<UUID, UnpluggedSession> persisted = GSON.fromJson(reader, SESSIONS_TYPE);

            if (persisted == null) {
                return;
            }

            persisted.forEach((uuid, session) -> {
                if (!session.isExpired()) {
                    this.sessions.put(uuid, session);
                }
            });

            this.logger.info("Loaded {} unplugged session(s) from {}, pruned {}.", this.sessions.size(), this.file, persisted.size() - this.sessions.size());
        } catch (IOException | JsonParseException exception) {
            this.logger.warn("Could not read {} - starting with no sessions.", this.file, exception);
        }
    }

    private void save() {
        final var temp = this.file.resolveSibling(SESSIONS_FILE_NAME + ".tmp");

        try {
            Files.createDirectories(this.file.getParent());

            try (final var writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(this.sessions, SESSIONS_TYPE, writer);
            }

            Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            this.logger.warn("Could not write {} - sessions will not survive a restart.", this.file, exception);
        }
    }
}
