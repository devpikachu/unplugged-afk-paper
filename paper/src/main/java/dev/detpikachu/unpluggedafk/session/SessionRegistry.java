package dev.detpikachu.unpluggedafk.session;

import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class SessionRegistry {

    private static final SessionRegistry INSTANCE = new SessionRegistry();

    private final ConcurrentHashMap.KeySetView<UUID, Boolean> unplugging;
    private final ConcurrentHashMap<UUID, CompoundTag> snapshots;
    private final ConcurrentHashMap<UUID, UnpluggedServerPlayer> unplugged;

    private SessionRegistry() {
        this.unplugging = ConcurrentHashMap.newKeySet(16);
        this.unplugged = new ConcurrentHashMap<>(16, 0.9F, 1);
        this.snapshots = new ConcurrentHashMap<>(16, 0.9F, 1);
    }

    public static SessionRegistry getInstance() {
        return INSTANCE;
    }

    public boolean isUnplugging(UUID uuid) {
        return this.unplugging.contains(uuid);
    }

    public void markUnplugging(UUID uuid) {
        this.unplugging.add(uuid);
    }

    public int countUnplugging() {
        return this.unplugging.size();
    }

    public void clearUnplugging(UUID uuid) {
        this.unplugging.remove(uuid);
        this.snapshots.remove(uuid);
    }

    public void putSnapshot(UUID uuid, CompoundTag snapshot) {
        this.snapshots.put(uuid, snapshot);
    }

    public @Nullable CompoundTag consumeSnapshot(UUID uuid) {
        return this.snapshots.remove(uuid);
    }

    public boolean isUnplugged(UUID uuid) {
        return this.unplugged.containsKey(uuid);
    }

    public int count() {
        return this.unplugged.size() + this.unplugging.size();
    }

    public @Nullable UnpluggedServerPlayer find(UUID uuid) {
        return this.unplugged.get(uuid);
    }

    public Collection<UnpluggedServerPlayer> all() {
        return Collections.unmodifiableCollection(this.unplugged.values());
    }

    public void add(UnpluggedServerPlayer bot) {
        this.unplugged.put(bot.getUUID(), bot);
    }

    public void remove(UnpluggedServerPlayer bot) {
        this.unplugged.remove(bot.getUUID());
    }

    public void removeAll() {
        this.unplugging.clear();
        this.snapshots.clear();
        this.unplugged.clear();
    }
}
