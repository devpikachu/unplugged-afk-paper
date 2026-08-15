package dev.detpikachu.unpluggedAfk;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedAfk.network.UnpluggedGamePacketListener;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.util.KeepAlive;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public final class UnpluggedPlayerManager {

    private static final UnpluggedPlayerManager INSTANCE = new UnpluggedPlayerManager();

    private final ConcurrentHashMap<UUID, UnpluggedServerPlayer> players;

    private UnpluggedPlayerManager() {
        this.players = new ConcurrentHashMap<>(16, 0.9F, 1);
    }

    public static UnpluggedPlayerManager getInstance() {
        return INSTANCE;
    }

    public boolean contains(UUID uuid) {
        return this.players.containsKey(uuid);
    }

    public @Nullable UnpluggedServerPlayer get(UUID uuid) {
        return this.players.get(uuid);
    }

    public void remove(UnpluggedServerPlayer player) {
        this.players.remove(player.getUUID());
    }

    public UnpluggedServerPlayer unplugPlayer(MinecraftServer server, ServerLevel level, ServerPlayer player, int durationMins, String reason) {
        return this.create(server, level, player.gameProfile, player.clientInformation(), durationMins, reason);
    }

    public UnpluggedServerPlayer createFake(MinecraftServer server, ServerLevel level, UUID uuid, String name, int durationMins, String reason) {
        final var profile = new GameProfile(uuid, name);
        final var clientInformation = ClientInformation.createDefault();

        final var unpluggedPlayer = this.create(server, level, profile, clientInformation, durationMins, reason);
        unpluggedPlayer.setIsFake(true);

        return unpluggedPlayer;
    }

    private UnpluggedServerPlayer create(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation clientInformation, int durationMins, String reason) {
        final var cookie = new CommonListenerCookie(profile, 0, clientInformation, true, null, new HashSet<>(), new KeepAlive());
        final var connection = new UnpluggedConnection(PacketFlow.SERVERBOUND);

        final var unpluggedPlayer = new UnpluggedServerPlayer(server, level, profile, clientInformation);
        this.players.put(unpluggedPlayer.getUUID(), unpluggedPlayer);

        server.getPlayerList().placeNewPlayer(connection, unpluggedPlayer, cookie);
        unpluggedPlayer.connection = new UnpluggedGamePacketListener(server, connection, unpluggedPlayer, cookie);
        unpluggedPlayer.gameMode.changeGameModeForPlayer(GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);
        unpluggedPlayer.setDurationMins(durationMins);
        unpluggedPlayer.setReason(reason);

        return unpluggedPlayer;
    }
}
