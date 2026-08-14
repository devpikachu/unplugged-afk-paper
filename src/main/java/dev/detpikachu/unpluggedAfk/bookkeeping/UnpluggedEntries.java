package dev.detpikachu.unpluggedAfk.bookkeeping;

import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class UnpluggedEntries {

    private static final UnpluggedEntries INSTANCE = new UnpluggedEntries();

    private final ConcurrentHashMap<UUID, UnpluggedEntry> entries;

    private UnpluggedEntries() {
        this.entries = new ConcurrentHashMap<>(16, 0.9F, 1);
    }

    public static UnpluggedEntries getInstance() {
        return INSTANCE;
    }

    public boolean contains(@Nonnull UUID uuid) {
        return this.entries.containsKey(uuid);
    }

    public @Nullable UnpluggedEntry get(@Nonnull UnpluggedServerPlayer player) {
        return this.get(player.getUUID());
    }

    public @Nullable UnpluggedEntry get(UUID uuid) {
        return this.entries.get(uuid);
    }

    public UnpluggedEntry add(@Nonnull UnpluggedServerPlayer player) {
        var uuid = player.getUUID();
        if (this.contains(uuid)) {
            return this.get(uuid);
        }

        var entry = new UnpluggedEntry(player);
        this.entries.put(uuid, entry);

        return entry;
    }

    public @Nullable UnpluggedEntry remove(@Nonnull UUID uuid) {
        return this.entries.remove(uuid);
    }
}
