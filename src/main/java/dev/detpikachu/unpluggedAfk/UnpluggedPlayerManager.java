package dev.detpikachu.unpluggedAfk;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.bookkeeping.UnpluggedPlayers;
import dev.detpikachu.unpluggedAfk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedAfk.network.UnpluggedGamePacketListener;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.util.KeepAlive;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public final class UnpluggedPlayerManager {

    public static UnpluggedServerPlayer createDummy(MinecraftServer server, ServerLevel level, UUID uuid, String name) {
        final var profile = new GameProfile(uuid, name);
        final var clientInformation = ClientInformation.createDefault();
        final var cookie = new CommonListenerCookie(profile, 0, clientInformation, true, null, new HashSet<>(), new KeepAlive());
        final var connection = new UnpluggedConnection(PacketFlow.SERVERBOUND);

        final var unpluggedPlayer = new UnpluggedServerPlayer(server, level, profile, clientInformation);
        UnpluggedPlayers.getInstance().add(unpluggedPlayer);

        server.getPlayerList().placeNewPlayer(connection, unpluggedPlayer, cookie);
        unpluggedPlayer.connection = new UnpluggedGamePacketListener(server, connection, unpluggedPlayer, cookie);
        unpluggedPlayer.gameMode.changeGameModeForPlayer(GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);

        return unpluggedPlayer;
    }
}
