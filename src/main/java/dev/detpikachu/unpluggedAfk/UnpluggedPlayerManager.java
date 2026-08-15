package dev.detpikachu.unpluggedAfk;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedAfk.network.UnpluggedGamePacketListener;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.util.KeepAlive;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
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

    public boolean isPending(UUID uuid) {
        return this.pending.contains(uuid);
    }

    public void forEach(Consumer<UnpluggedServerPlayer> action) {
        this.players.values().forEach(action);
    }

    public void remove(UnpluggedServerPlayer player) {
        this.players.remove(player.getUUID());
    }

    public UnpluggedServerPlayer unplugPlayer(MinecraftServer server, ServerLevel level, ServerPlayer player, int durationMins, String reason) {
        try {
            pending.add(player.getUUID());
            // TODO: Configurable message
            player.getBukkitEntity().kick(Component.text("AFK"), PlayerKickEvent.Cause.PLUGIN);

            if (server.getPlayerList().getPlayer(player.getUUID()) != null) {
                throw new RuntimeException(EXCEPTION_FAILED_TO_DISCONNECT);
            }

            final var unpluggedPlayer = this.create(server, level, player.gameProfile, player.clientInformation(), durationMins, reason);

            // Load player NBT
            try (ProblemReporter.ScopedCollector logger = new ProblemReporter.ScopedCollector(unpluggedPlayer.problemPath(), UnpluggedAfk.LOGGER)) {
                final var opt = unpluggedPlayer
                        .level()
                        .getServer()
                        .getPlayerList()
                        .loadPlayerData(unpluggedPlayer.nameAndId())
                        .map(nbt -> TagValueInput.create(logger, unpluggedPlayer.registryAccess(), nbt));

                opt.ifPresent(data -> {
                    unpluggedPlayer.load(data);
                    unpluggedPlayer.loadAndSpawnEnderPearls(data);
                    unpluggedPlayer.loadAndSpawnParentVehicle(data);
                });
            }

            return unpluggedPlayer;
        } finally {
            this.pending.remove(player.getUUID());
        }
    }

    public UnpluggedServerPlayer createFake(MinecraftServer server, ServerLevel level, UUID uuid, String name, int durationMins, String reason) {
        final var profile = new GameProfile(uuid, name);
        final var clientInformation = ClientInformation.createDefault();

        final var unpluggedPlayer = this.create(server, level, profile, clientInformation, durationMins, reason);

        unpluggedPlayer.gameMode.changeGameModeForPlayer(GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);
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
        unpluggedPlayer.setDurationMins(durationMins);
        unpluggedPlayer.setReason(reason);

        return unpluggedPlayer;
    }
}
