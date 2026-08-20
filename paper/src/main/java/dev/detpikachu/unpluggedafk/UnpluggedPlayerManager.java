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
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.Connection;
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
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.CHANNEL_SESSIONS;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.EXCEPTION_FAILED_TO_DISCONNECT;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.MESSAGE_SESSION_START;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.SUBCHANNEL_KICK_PLAYER_RAW;

public final class UnpluggedPlayerManager {

    private static final UnpluggedPlayerManager INSTANCE = new UnpluggedPlayerManager();

    private static final int SPAWN_SETTLE_TICKS = 1;
    private static final int SPAWN_TIMEOUT_TICKS = 100;

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
        return this.players.size() + this.pending.size();
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
        final var name = player.getName().getString();
        final var level = player.level();
        final var server = level.getServer();
        final var message = UnpluggedChatFormatting.formatUnplugged(session.durationMins(), session.reason());
        final var oldConnection = player.connection.connection;
        final var clientInformation = player.clientInformation();
        final var gameProfile = player.gameProfile;

        UnpluggedAfk.LOGGER.info("Unplugging {} ({}) for {} minute(s): {}", name, uuid, session.durationMins(), session.reason());
        if (UnpluggedOptions.getInstance().isDebug()) {
            UnpluggedDumpWriter.write(player.getBukkitEntity(), session);
        }

        var spawnScheduled = false;

        try {
            pending.add(uuid);
            this.announceSessionToProxy(player.getBukkitEntity(), session);
            this.disconnectFromProxy(player.getBukkitEntity(), message);
            player.getBukkitEntity().kick(message, PlayerKickEvent.Cause.PLUGIN);

            if (server.getPlayerList().getPlayer(uuid) != null) {
                UnpluggedAfk.LOGGER.error("{} ({}) is still connected after the kick, so nothing holds their spot.", name, uuid);
                throw new UnplugFailedException(EXCEPTION_FAILED_TO_DISCONNECT);
            }

            this.spawnWhenSettled(uuid, name, level, gameProfile, clientInformation, session, oldConnection);
            spawnScheduled = true;
        } finally {
            if (!spawnScheduled) {
                this.pending.remove(uuid);
            }
        }
    }

    public UnpluggedServerPlayer createFake(ServerLevel level, UnpluggedSession session) {
        final var unpluggedPlayer = this.create(level, UnpluggedFakeIdentity.random().toProfile(), ClientInformation.createDefault(), session);

        unpluggedPlayer.gameMode.changeGameModeForPlayer(GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);
        unpluggedPlayer.getBukkitEntity().setPersistent(false);

        return unpluggedPlayer;
    }

    private void spawnWhenSettled(UUID uuid, String name, ServerLevel level, GameProfile profile,
                                  ClientInformation clientInformation, UnpluggedSession session,
                                  Connection oldConnection) {
        final var server = level.getServer();
        final var plugin = JavaPlugin.getPlugin(UnpluggedAfk.class);
        final var waited = new AtomicInteger();
        final var settled = new AtomicInteger();

        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (server.getPlayerList().getPlayer(uuid) != null) {
                task.cancel();
                this.pending.remove(uuid);
                UnpluggedAfk.LOGGER.warn("{} ({}) reconnected before their bot existed, so none was created.", name, uuid);
                return;
            }

            if (oldConnection.isConnected()) {
                if (waited.incrementAndGet() < SPAWN_TIMEOUT_TICKS) {
                    return;
                }

                UnpluggedAfk.LOGGER.warn("{} ({}) still had an open connection after {} tick(s). Creating their bot anyway.", name, uuid, SPAWN_TIMEOUT_TICKS);
            } else if (settled.getAndIncrement() < SPAWN_SETTLE_TICKS) {
                return;
            }

            task.cancel();
            this.pending.remove(uuid);
            this.spawn(uuid, name, level, profile, clientInformation, session);
        }, 1L, 1L);
    }

    private void spawn(UUID uuid, String name, ServerLevel level, GameProfile profile, ClientInformation clientInformation, UnpluggedSession session) {
        final UnpluggedServerPlayer unpluggedPlayer;

        try {
            unpluggedPlayer = this.create(level, profile, clientInformation, session);
            unpluggedPlayer.loadPersistedData();
        } catch (RuntimeException exception) {
            UnpluggedAfk.LOGGER.error("{} ({}) was disconnected but no bot could be created to hold their spot.", name, uuid, exception);
            return;
        }

        UnpluggedAfk.LOGGER.info(
                "{} ({}) is unplugged at {}, {}, {} in {}. {} of {} slot(s) in use.",
                name,
                uuid,
                (int) unpluggedPlayer.getX(),
                (int) unpluggedPlayer.getY(),
                (int) unpluggedPlayer.getZ(),
                unpluggedPlayer.level().dimension().identifier(),
                this.count(),
                UnpluggedOptions.getInstance().getMaxUnpluggedPlayers()
        );
    }

    private UnpluggedServerPlayer create(ServerLevel level, GameProfile profile, ClientInformation clientInformation, UnpluggedSession session) {
        final var server = level.getServer();
        final var cookie = new CommonListenerCookie(profile, 0, clientInformation, true, null, new HashSet<>(), new KeepAlive());
        final var connection = new UnpluggedConnection(PacketFlow.SERVERBOUND);

        final var unpluggedPlayer = new UnpluggedServerPlayer(server, level, profile, clientInformation, session);

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

    private void announceSessionToProxy(Player player, UnpluggedSession session) {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            UnpluggedAfk.logDebug("Proxy mode is off, so no SESSION_START was announced for {}.", player.getName());
            return;
        }

        final var out = ByteStreams.newDataOutput();
        out.writeUTF(MESSAGE_SESSION_START);
        out.writeInt(session.durationMins());

        player.sendPluginMessage(JavaPlugin.getPlugin(UnpluggedAfk.class), CHANNEL_SESSIONS, out.toByteArray());
        UnpluggedAfk.LOGGER.info("Announced SESSION_START for {} ({}) to the proxy: {} minute(s).", player.getName(), player.getUniqueId(), session.durationMins());
    }
}
