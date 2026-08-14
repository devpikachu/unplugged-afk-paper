package dev.detpikachu.unpluggedAfk.bookkeeping;

import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class UnpluggedPlayers {

    private static final UnpluggedPlayers INSTANCE = new UnpluggedPlayers();

    private final ConcurrentHashMap<UUID, UnpluggedServerPlayer> players;

    private UnpluggedPlayers() {
        this.players = new ConcurrentHashMap<>(16, 0.9F, 1);
    }

    public static UnpluggedPlayers getInstance() {
        return INSTANCE;
    }

    public boolean contains(@Nonnull UUID uuid) {
        return this.players.containsKey(uuid);
    }

    public @Nullable UnpluggedServerPlayer get(UUID uuid) {
        return this.players.get(uuid);
    }

    public void add(@Nonnull UnpluggedServerPlayer player) {
        this.players.put(player.getUUID(), player);
    }

    public void remove(@Nonnull UUID uuid) {
        this.players.remove(uuid);
    }
}
