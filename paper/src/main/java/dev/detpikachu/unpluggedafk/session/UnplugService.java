package dev.detpikachu.unpluggedafk.session;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedafk.DumpWriter;
import dev.detpikachu.unpluggedafk.KickReasons;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.api.events.PlayerUnplugEvent;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionAck;
import dev.detpikachu.unpluggedafk.config.Config;
import dev.detpikachu.unpluggedafk.exceptions.ProxyUnavailableException;
import dev.detpikachu.unpluggedafk.exceptions.UnplugCancelledException;
import dev.detpikachu.unpluggedafk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedafk.format.ChatMessages;
import dev.detpikachu.unpluggedafk.player.BotFactory;
import dev.detpikachu.unpluggedafk.player.FakeIdentity;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;

import static net.kyori.adventure.text.Component.text;

@ApiStatus.Internal
public final class UnplugService {

    public static final String END_ABORTED = "ABORTED";

    public static void unplug(ServerPlayer player, Session session) throws UnplugFailedException {
        final var registry = SessionRegistry.getInstance();
        final var uuid = player.getUUID();
        final var name = player.getPlainTextName();
        final var event = new PlayerUnplugEvent(player.getBukkitEntity(), session.durationMins(), session.reason());

        if (!event.callEvent()) {
            Log.info("Refused to unplug {} ({}): another plugin cancelled the request.", name, uuid);
            throw new UnplugCancelledException(event.getCancelMessage());
        }

        final var client = UnpluggedAfk.getInstance().getLinkClient();
        final var proxied = UnpluggedAfk.isProxyMode();

        if (proxied && !client.isReady()) {
            throw new ProxyUnavailableException(uuid, name);
        }

        Log.info("Unplugging {} ({}) for {} minute(s): {}", name, uuid, session.durationMins(), session.reason());

        if (Config.get().isDebug()) {
            DumpWriter.write(player.getBukkitEntity(), session);
        }

        registry.markUnplugging(uuid);

        if (!proxied) {
            commit(player, session);
            return;
        }

        final var profile = player.getGameProfile();

        client.startSession(profile, session, ack -> onAcknowledged(profile, ack, () -> resume(player, session, ack)));
    }

    public static void spawnFake(ServerPlayer executor, Session session) throws ProxyUnavailableException {
        final var fake = new FakeSpawn(
                executor.level(),
                FakeIdentity.random().toProfile(),
                executor.position(),
                executor.getYRot(),
                executor.getXRot(),
                session);

        if (!UnpluggedAfk.isProxyMode()) {
            fake.spawn();
            return;
        }

        final var client = UnpluggedAfk.getInstance().getLinkClient();
        final var profile = fake.profile();

        if (!client.isReady()) {
            throw new ProxyUnavailableException(profile.id(), profile.name());
        }

        SessionRegistry.getInstance().markUnplugging(profile.id());
        client.startSession(
                profile, session, ack -> onAcknowledged(profile, ack, () -> resumeFake(executor, fake, ack)));
    }

    private static void onAcknowledged(GameProfile profile, SessionAck ack, Runnable resume) {
        final var plugin = UnpluggedAfk.getInstance();

        if (!plugin.isEnabled()) {
            abandon(profile, ack);
            return;
        }

        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> resume.run());
    }

    private static void abandon(GameProfile profile, SessionAck ack) {
        final var registry = SessionRegistry.getInstance();
        final var uuid = profile.id();

        registry.clearUnplugging(uuid);

        if (!ack.accepted()) {
            return;
        }

        Log.warn(
                "The proxy acknowledged the unplug of {} ({}) while the plugin was disabling, so it is undone.",
                profile.name(),
                uuid);
        UnpluggedAfk.getInstance().getLinkClient().endSession(uuid, END_ABORTED);
    }

    private static void resumeFake(ServerPlayer executor, FakeSpawn fake, SessionAck ack) {
        final var uuid = fake.profile().id();
        final var name = fake.profile().name();

        SessionRegistry.getInstance().clearUnplugging(uuid);

        if (!ack.accepted()) {
            Log.warn("The proxy refused the fake bot {} ({}): {}", name, uuid, ack.reason());

            if (!executor.hasDisconnected()) {
                executor.getBukkitEntity().sendMessage(ChatMessages.formatUnplugRefused(ack.reason()));
            }

            return;
        }

        try {
            fake.spawn();
        } catch (RuntimeException exception) {
            Log.error(
                    "Failed to spawn fake bot {} ({}) after the proxy acknowledged the session.",
                    name,
                    uuid,
                    exception);

            final var bot = SessionRegistry.getInstance().find(uuid);

            if (bot != null) {
                bot.deferredDisconnect(text(KickReasons.SPAWN_FAILED), Reason.SPAWN_FAILED);
                return;
            }

            UnpluggedAfk.getInstance().getLinkClient().endSession(uuid, END_ABORTED);
        }
    }

    private static void resume(ServerPlayer player, Session session, SessionAck ack) {
        final var registry = SessionRegistry.getInstance();
        final var client = UnpluggedAfk.getInstance().getLinkClient();
        final var uuid = player.getUUID();
        final var name = player.getPlainTextName();

        if (!ack.accepted()) {
            Log.warn("The proxy refused the unplug of {} ({}): {}", name, uuid, ack.reason());
            registry.clearUnplugging(uuid);
            player.getBukkitEntity().sendMessage(ChatMessages.formatUnplugRefused(ack.reason()));
            return;
        }

        if (player.hasDisconnected() || player.isDeadOrDying()) {
            Log.warn("{} ({}) was no longer eligible when the proxy answered, so the session is undone.", name, uuid);
            client.endSession(uuid, END_ABORTED);
            registry.clearUnplugging(uuid);
            return;
        }

        commit(player, session);
    }

    private static void commit(ServerPlayer player, Session session) {
        final var registry = SessionRegistry.getInstance();
        final var uuid = player.getUUID();
        final var name = player.getPlainTextName();
        final var level = player.level();
        final var message = ChatMessages.formatUnplugged(session.durationMins(), session.reason());
        final var oldConnection = player.connection.connection;
        final var clientInformation = player.clientInformation();
        final var gameProfile = player.gameProfile;
        final var channels = player.getBukkitEntity().getListeningPluginChannels();

        var spawnScheduled = false;

        try {
            registry.markCommitting(uuid);
            player.getBukkitEntity().kick(message, PlayerKickEvent.Cause.PLUGIN);

            if (player.quitReason != PlayerQuitEvent.QuitReason.KICKED) {
                Log.warn(
                        "A plugin cancelled the kick of {} ({}), but the unplug is already committed. Forcing it.",
                        name,
                        uuid);
                forceDisconnect(player, message);
            }

            BotFactory.spawnWhenSettled(level, gameProfile, clientInformation, channels, session, oldConnection);
            spawnScheduled = true;
        } finally {
            if (!spawnScheduled) {
                registry.clearUnplugging(uuid);
            }
        }
    }

    private static void forceDisconnect(ServerPlayer player, Component message) {
        player.connection.connection.disconnect(new DisconnectionDetails(PaperAdventure.asVanilla(message)));
    }

    private record FakeSpawn(
            ServerLevel level, GameProfile profile, Vec3 position, float yRot, float xRot, Session session) {

        void spawn() {
            BotFactory.spawnFake(this.level, this.profile, this.position, this.yRot, this.xRot, this.session);
        }
    }
}
