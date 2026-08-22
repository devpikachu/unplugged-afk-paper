package dev.detpikachu.unpluggedafk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.network.GamePacketListener;
import dev.detpikachu.unpluggedafk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedafk.session.Session;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.papermc.paper.util.KeepAlive;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class BotFactory {

    private static final int SPAWN_SETTLE_TICKS = 1;
    private static final int SPAWN_TIMEOUT_TICKS = 100;

    public static void spawnWhenSettled(
            UUID uuid,
            String name,
            ServerLevel level,
            GameProfile profile,
            ClientInformation clientInformation,
            Session session,
            Connection oldConnection
    ) {
        final var registry = SessionRegistry.getInstance();
        final var server = level.getServer();
        final var plugin = JavaPlugin.getPlugin(UnpluggedAfk.class);
        final var waited = new AtomicInteger();
        final var settled = new AtomicInteger();

        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (server.getPlayerList().getPlayer(uuid) != null) {
                task.cancel();
                registry.clearUnplugging(uuid);
                LOGGER.warn("{} ({}) reconnected before their bot existed, so none was created.", name, uuid);
                return;
            }

            if (oldConnection.isConnected()) {
                if (waited.incrementAndGet() < SPAWN_TIMEOUT_TICKS) {
                    return;
                }

                LOGGER.warn("{} ({}) still had an open connection after {} tick(s). Creating their bot anyway.", name, uuid, SPAWN_TIMEOUT_TICKS);
            } else if (settled.getAndIncrement() < SPAWN_SETTLE_TICKS) {
                return;
            }

            task.cancel();
            registry.clearUnplugging(uuid);

            final UnpluggedServerPlayer bot;
            try {
                bot = spawn(level, profile, clientInformation, session);
                bot.loadPersistedData();
            } catch (RuntimeException exception) {
                LOGGER.error("{} ({}) was disconnected but no bot could be created to hold their spot.", name, uuid, exception);
                return;
            }

            LOGGER.info(
                    "{} ({}) is unplugged at {}, {}, {} in {}. {} of {} slot(s) in use.",
                    name,
                    uuid,
                    (int) bot.getX(),
                    (int) bot.getY(),
                    (int) bot.getZ(),
                    bot.level().dimension().identifier(),
                    SessionRegistry.getInstance().count(),
                    Options.getInstance().getMaxUnpluggedPlayers()
            );
        }, 1L, 1L);
    }

    public static void spawnFake(ServerLevel level, Vec3 position, float yRot, float xRot, Session session) {
        final var bot = spawn(level, FakeIdentity.random().toProfile(), ClientInformation.createDefault(), session);

        bot.gameMode.changeGameModeForPlayer(GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);
        bot.getBukkitEntity().setPersistent(false);
        bot.connection.teleport(position.x, position.y, position.z, yRot, xRot);

        LOGGER.info(
                "Spawned fake bot {} at {}, {}, {} in {} for {} minute(s).",
                bot.getName().getString(),
                (int) position.x(),
                (int) position.y(),
                (int) position.z(),
                level.dimension().identifier(),
                session.durationMins()
        );
    }

    private static UnpluggedServerPlayer spawn(ServerLevel level, GameProfile profile,
                                               ClientInformation clientInformation, Session session) {
        final var server = level.getServer();
        final var cookie = new CommonListenerCookie(profile, 0, clientInformation, true, null, new HashSet<>(), new KeepAlive());
        final var connection = new UnpluggedConnection(server, PacketFlow.SERVERBOUND);

        final var bot = new UnpluggedServerPlayer(server, level, profile, clientInformation, session);

        server.getPlayerList().placeNewPlayer(connection, bot, cookie);
        bot.connection = new GamePacketListener(server, connection, bot, cookie);

        SessionRegistry.getInstance().add(bot);

        return bot;
    }
}
