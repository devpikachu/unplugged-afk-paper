package dev.detpikachu.unpluggedafk;

import com.google.common.io.ByteStreams;
import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedafk.config.UnpluggedOptions;
import dev.detpikachu.unpluggedafk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedafk.formatting.UnpluggedChatFormatting;
import dev.detpikachu.unpluggedafk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedafk.network.UnpluggedGamePacketListener;
import dev.detpikachu.unpluggedafk.player.UnpluggedFakeIdentity;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.player.UnpluggedSession;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.util.KeepAlive;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static dev.detpikachu.unpluggedafk.UnpluggedConstants.CHANNEL_BUNGEE;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.EXCEPTION_FAILED_TO_DISCONNECT;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.SUBCHANNEL_KICK_PLAYER_RAW;

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
        final var message = UnpluggedChatFormatting.formatUnplugged(session.durationMins(), session.reason());

        if (UnpluggedOptions.getInstance().isDebug()) {
            UnpluggedDumpWriter.write(player.getBukkitEntity(), session);
        }

        try {
            pending.add(uuid);
            this.disconnectFromProxy(player.getBukkitEntity(), message);
            player.getBukkitEntity().kick(message, PlayerKickEvent.Cause.PLUGIN);

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

    private void disconnectFromProxy(Player player, Component message) {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            return;
        }

        final var out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_KICK_PLAYER_RAW);
        out.writeUTF(player.getName());
        out.writeUTF(GsonComponentSerializer.gson().serialize(message));

        player.sendPluginMessage(JavaPlugin.getPlugin(UnpluggedAfk.class), CHANNEL_BUNGEE, out.toByteArray());
    }
}
