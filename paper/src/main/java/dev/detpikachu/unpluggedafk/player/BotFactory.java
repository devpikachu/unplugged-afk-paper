package dev.detpikachu.unpluggedafk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedafk.KickReasons;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerSpawnEvent;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import dev.detpikachu.unpluggedafk.config.Config;
import dev.detpikachu.unpluggedafk.session.Session;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import dev.detpikachu.unpluggedafk.session.UnplugService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.papermc.paper.util.KeepAlive;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;

@ApiStatus.Internal
public final class BotFactory {

    private static final int SPAWN_SETTLE_TICKS = 1;
    private static final int SPAWN_TIMEOUT_TICKS = 100;
    private static final int SPAWN_CHUNK_RADIUS = 3;
    private static final int BOT_VIEW_DISTANCE = 2;

    public static void spawnWhenSettled(
            ServerLevel level,
            GameProfile profile,
            ClientInformation clientInformation,
            Set<String> channels,
            Session session,
            Connection oldConnection) {
        final var plugin = UnpluggedAfk.getInstance();
        final var scheduler = plugin.getServer().getGlobalRegionScheduler();
        final var deferred = new DeferredSpawn(level, profile, clientInformation, channels, session, oldConnection);

        scheduler.runAtFixedRate(plugin, deferred::tick, 1L, 1L);
    }

    public static void spawnFake(
            ServerLevel level, GameProfile profile, Vec3 position, float yRot, float xRot, Session session) {
        final var bot = spawn(level, profile, ClientInformation.createDefault(), Set.of(), session, null);

        bot.gameMode.changeGameModeForPlayer(
                GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);
        bot.getBukkitEntity().setPersistent(false);
        bot.connection.teleport(position.x, position.y, position.z, yRot, xRot);

        Log.info(
                "Spawned fake bot {} at {}, {}, {} in {} for {} minute(s).",
                bot.describe(),
                (int) position.x(),
                (int) position.y(),
                (int) position.z(),
                level.dimension().identifier(),
                session.durationMins());

        new UnpluggedPlayerSpawnEvent(bot.getBukkitEntity(), bot.toInfo()).callEvent();
    }

    private static UnpluggedServerPlayer spawn(
            ServerLevel level,
            GameProfile profile,
            ClientInformation clientInformation,
            Set<String> channels,
            Session session,
            @Nullable CompoundTag persistedData) {
        final var server = level.getServer();
        final var botInformation = withBotViewDistance(clientInformation);
        final var cookie = new CommonListenerCookie(
                profile, 0, botInformation, true, null, new HashSet<>(channels), new KeepAlive());
        final var connection = new UnpluggedConnection(server, PacketFlow.SERVERBOUND, profile.id());

        final var bot = new UnpluggedServerPlayer(server, level, profile, botInformation, session);
        final var registry = SessionRegistry.getInstance();

        registry.add(bot); // placeNewPlayer fires PlayerJoinEvent, where a late registration reads isUnplugged false.
        var placed = false;

        try (final var reporter = new ProblemReporter.ScopedCollector(bot.problemPath(), Log.logger())) {
            final var data = toValueInput(bot, reporter, persistedData);

            if (data != null) {
                bot.load(data);

                final var chunkPos = new ChunkPos(
                        SectionPos.blockToSectionCoord(bot.getBlockX()),
                        SectionPos.blockToSectionCoord(bot.getBlockZ()));

                level.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN, chunkPos, SPAWN_CHUNK_RADIUS);
                level.waitForEntities(chunkPos, SPAWN_CHUNK_RADIUS);
            }

            server.getPlayerList().placeNewPlayer(connection, bot, cookie);
            placed = true;

            if (bot.isRemoved()) {
                throw new IllegalStateException(
                        "A plugin removed bot " + bot.describe() + " during its own PlayerJoinEvent.");
            }

            bot.connection = new GamePacketListener(server, connection, bot, cookie);

            if (data != null) {
                bot.loadAndSpawnEnderPearls(data);
                bot.loadAndSpawnParentVehicle(data);
            }
        } catch (RuntimeException exception) {
            if (placed) {
                bot.deferredDisconnect(text(KickReasons.SPAWN_FAILED), Reason.SPAWN_FAILED);
                throw exception;
            }

            registry.remove(bot);
            throw exception;
        }

        return bot;
    }

    private static ClientInformation withBotViewDistance(ClientInformation clientInformation) {
        return new ClientInformation(
                clientInformation.language(),
                BOT_VIEW_DISTANCE,
                clientInformation.chatVisibility(),
                clientInformation.chatColors(),
                clientInformation.modelCustomisation(),
                clientInformation.mainHand(),
                clientInformation.textFilteringEnabled(),
                clientInformation.allowsListing(),
                clientInformation.particleStatus());
    }

    private static @Nullable ValueInput toValueInput(
            UnpluggedServerPlayer bot, ProblemReporter reporter, @Nullable CompoundTag persistedData) {
        return persistedData == null ? null : TagValueInput.create(reporter, bot.registryAccess(), persistedData);
    }

    private static final class DeferredSpawn {

        private final UUID uuid;
        private final String name;
        private final ServerLevel level;
        private final GameProfile profile;
        private final ClientInformation clientInformation;
        private final Set<String> channels;
        private final Session session;
        private final Connection oldConnection;

        private int waited = 0;
        private int settled = 0;

        private DeferredSpawn(
                ServerLevel level,
                GameProfile profile,
                ClientInformation clientInformation,
                Set<String> channels,
                Session session,
                Connection oldConnection) {
            this.uuid = profile.id();
            this.name = profile.name();
            this.level = level;
            this.profile = profile;
            this.clientInformation = clientInformation;
            this.channels = channels;
            this.session = session;
            this.oldConnection = oldConnection;
        }

        private void tick(ScheduledTask task) {
            if (this.hasReconnected()) {
                task.cancel();
                SessionRegistry.getInstance().clearUnplugging(this.uuid);
                Log.warn("{} ({}) reconnected before their bot existed, so none was created.", this.name, this.uuid);
                return;
            }

            if (!this.hasSettled()) {
                return;
            }

            task.cancel();
            final var persistedData = this.resolvePersistedData();
            SessionRegistry.getInstance().clearUnplugging(this.uuid);
            this.spawnBot(persistedData);
        }

        private boolean hasReconnected() {
            return this.level.getServer().getPlayerList().getPlayer(this.uuid) != null;
        }

        private boolean hasSettled() {
            if (!this.oldConnection.isConnected()) {
                return this.settled++ >= SPAWN_SETTLE_TICKS;
            }

            if (++this.waited < SPAWN_TIMEOUT_TICKS) {
                return false;
            }

            Log.warn(
                    "{} ({}) still had an open connection after {} tick(s). Creating their bot anyway.",
                    this.name,
                    this.uuid,
                    SPAWN_TIMEOUT_TICKS);
            return true;
        }

        private void spawnBot(@Nullable CompoundTag persistedData) {
            final UnpluggedServerPlayer bot;

            try {
                bot = spawn(
                        this.level, this.profile, this.clientInformation, this.channels, this.session, persistedData);
            } catch (RuntimeException exception) {
                Log.error(
                        "{} ({}) was disconnected but no bot could be created to hold their spot.",
                        this.name,
                        this.uuid,
                        exception);

                if (SessionRegistry.getInstance().find(this.uuid) == null) {
                    UnpluggedAfk.getInstance().getLinkClient().endSession(this.uuid, UnplugService.END_ABORTED);
                }

                return;
            }

            Log.info(
                    "{} is unplugged at {}, {}, {} in {}. {} of {} slot(s) in use.",
                    bot.describe(),
                    (int) bot.getX(),
                    (int) bot.getY(),
                    (int) bot.getZ(),
                    bot.level().dimension().identifier(),
                    SessionRegistry.getInstance().count(),
                    Config.get().getMaxUnpluggedPlayers());

            new UnpluggedPlayerSpawnEvent(bot.getBukkitEntity(), bot.toInfo()).callEvent();
        }

        private @Nullable CompoundTag resolvePersistedData() {
            final var snapshot = SessionRegistry.getInstance().consumeSnapshot(this.uuid);

            if (snapshot != null) {
                return snapshot;
            }

            Log.warn("No snapshot for {} ({}). Falling back to their playerdata file.", this.name, this.uuid);
            return this.readPersistedData();
        }

        private @Nullable CompoundTag readPersistedData() {
            final var playerList = this.level.getServer().getPlayerList();
            final var persistedData = playerList.loadPlayerData(new NameAndId(this.profile));

            if (persistedData.isEmpty()) {
                Log.warn(
                        "No persisted data for {} ({}). The bot holds world spawn with an empty inventory.",
                        this.name,
                        this.uuid);
                return null;
            }

            return persistedData.get();
        }
    }
}
