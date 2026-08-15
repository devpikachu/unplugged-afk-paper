package dev.detpikachu.unpluggedAfk;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedAfk.network.UnpluggedGamePacketListener;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.util.KeepAlive;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import static net.minecraft.world.entity.Avatar.DATA_PLAYER_MODE_CUSTOMISATION;

public final class UnpluggedPlayerManager {

    private static final UnpluggedPlayerManager INSTANCE = new UnpluggedPlayerManager();

    private final ConcurrentHashMap<UUID, UnpluggedServerPlayer> players;

    private UnpluggedPlayerManager() {
        this.players = new ConcurrentHashMap<>(16, 0.9F, 1);
    }

    public static UnpluggedPlayerManager getInstance() {
        return INSTANCE;
    }

    public void remove(UnpluggedServerPlayer player) {
        this.players.remove(player.getUUID());
    }

    public UnpluggedServerPlayer unplugPlayer(MinecraftServer server, ServerLevel level, ServerPlayer player, int durationMins, String reason) {
        final var unpluggedPlayer = this.create(server, level, player.gameProfile, player.clientInformation(), durationMins, reason);

        unpluggedPlayer.setChatSession(player.getChatSession());
        unpluggedPlayer.setHealth(player.getHealth());
        unpluggedPlayer.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        unpluggedPlayer.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer(), PlayerGameModeChangeEvent.Cause.PLUGIN, null);
        unpluggedPlayer.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(.6f);
        unpluggedPlayer.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));

        // Survival players shouldn't be able to fly or be invulnerable
        if (unpluggedPlayer.gameMode.isSurvival()) {
            unpluggedPlayer.getAbilities().flying = false;
            unpluggedPlayer.setInvulnerable(false);
        } else {
            unpluggedPlayer.getAbilities().flying = player.getAbilities().flying;
        }

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
