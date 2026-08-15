package dev.detpikachu.unpluggedAfk;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedAfk.formatting.UnpluggedChatFormatting;
import dev.detpikachu.unpluggedAfk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedAfk.network.UnpluggedGamePacketListener;
import dev.detpikachu.unpluggedAfk.player.UnpluggedFakeIdentity;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedAfk.player.UnpluggedSession;
import io.papermc.paper.util.KeepAlive;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerKickEvent;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.EXCEPTION_FAILED_TO_DISCONNECT;

public final class UnpluggedPlayerManager {

    private static final UnpluggedPlayerManager INSTANCE = new UnpluggedPlayerManager();

    private final ConcurrentHashMap.KeySetView<UUID, Boolean> pending;
    private final ConcurrentHashMap<UUID, UnpluggedServerPlayer> players;

    private UnpluggedPlayerManager() {
        this.pending = ConcurrentHashMap.newKeySet(16);
        this.players = new ConcurrentHashMap<>(16, 0.9F, 1);
    }

    public static UnpluggedPlayerManager getInstance() {
        return INSTANCE;
    }

    public int count() {
        return this.players.size();
    }

    public boolean isPending(UUID uuid) {
        return this.pending.contains(uuid);
    }

    public Collection<UnpluggedServerPlayer> getPlayers() {
        return Collections.unmodifiableCollection(this.players.values());
    }

    public void remove(UnpluggedServerPlayer player) {
        this.players.remove(player.getUUID());
    }

    public void removeAll() {
        this.players.clear();
    }

    public void createPlayer(ServerPlayer player, UnpluggedSession session) throws UnplugFailedException {
        final var uuid = player.getUUID();
        final var level = player.level();
        final var server = level.getServer();

        try {
            pending.add(uuid);
            player.getBukkitEntity().kick(
                    UnpluggedChatFormatting.formatUnplugged(session.durationMins(), session.reason()),
                    PlayerKickEvent.Cause.PLUGIN);

            if (server.getPlayerList().getPlayer(player.getUUID()) != null) {
                throw new UnplugFailedException(EXCEPTION_FAILED_TO_DISCONNECT);
            }

            this.create(level, player.gameProfile, player.clientInformation(), session).loadPersistedData();
        } finally {
            this.pending.remove(player.getUUID());
        }
    }

    public UnpluggedServerPlayer createFake(ServerLevel level, UnpluggedSession session) {
        final var unpluggedPlayer = this.create(level, UnpluggedFakeIdentity.random().toProfile(), ClientInformation.createDefault(), session);

        unpluggedPlayer.gameMode.changeGameModeForPlayer(GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);
        unpluggedPlayer.getBukkitEntity().setPersistent(false);

        return unpluggedPlayer;
    }

    private UnpluggedServerPlayer create(ServerLevel level, GameProfile profile, ClientInformation clientInformation, UnpluggedSession session) {
        final var server = level.getServer();
        final var cookie = new CommonListenerCookie(profile, 0, clientInformation, true, null, new HashSet<>(), new KeepAlive());
        final var connection = new UnpluggedConnection(PacketFlow.SERVERBOUND);

        final var unpluggedPlayer = new UnpluggedServerPlayer(server, level, profile, clientInformation, connection, session);

        server.getPlayerList().placeNewPlayer(connection, unpluggedPlayer, cookie);
        unpluggedPlayer.connection = new UnpluggedGamePacketListener(server, connection, unpluggedPlayer, cookie);

        this.players.put(unpluggedPlayer.getUUID(), unpluggedPlayer);

        return unpluggedPlayer;
    }
}
