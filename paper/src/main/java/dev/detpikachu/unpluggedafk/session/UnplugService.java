package dev.detpikachu.unpluggedafk.session;

import dev.detpikachu.unpluggedafk.DumpWriter;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.api.events.PlayerUnplugEvent;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionAck;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.exceptions.PlayerStillConnectedException;
import dev.detpikachu.unpluggedafk.exceptions.ProxyUnavailableException;
import dev.detpikachu.unpluggedafk.exceptions.UnplugCancelledException;
import dev.detpikachu.unpluggedafk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import dev.detpikachu.unpluggedafk.player.BotFactory;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class UnplugService {

    private static final String END_ABORTED = "ABORTED";

    public static void unplug(ServerPlayer player, Session session) throws UnplugFailedException {
        final var registry = SessionRegistry.getInstance();
        final var uuid = player.getUUID();
        final var name = player.getPlainTextName();
        final var event = new PlayerUnplugEvent(player.getBukkitEntity(), session.durationMins(), session.reason());

        if (!event.callEvent()) {
            LOGGER.info("Refused to unplug {} ({}): another plugin cancelled the request.", name, uuid);
            throw new UnplugCancelledException(event.getCancelMessage());
        }

        final var client = UnpluggedAfk.getInstance().getLinkClient();
        final var proxied = UnpluggedAfk.isProxyMode();

        if (proxied && !client.isReady()) {
            throw new ProxyUnavailableException(uuid, name);
        }

        LOGGER.info("Unplugging {} ({}) for {} minute(s): {}", name, uuid, session.durationMins(), session.reason());

        if (Options.getInstance().isDebug()) {
            DumpWriter.write(player.getBukkitEntity(), session);
        }

        registry.markUnplugging(uuid);

        if (!proxied) {
            commit(player, session);
            return;
        }

        client.startSession(player, session.remaining().toSeconds(), ack -> onAcknowledged(player, session, ack));
    }

    private static void onAcknowledged(ServerPlayer player, Session session, SessionAck ack) {
        final var plugin = UnpluggedAfk.getInstance();

        if (!plugin.isEnabled()) {
            resume(player, session, ack);
            return;
        }

        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> resume(player, session, ack));
    }

    private static void resume(ServerPlayer player, Session session, SessionAck ack) {
        final var registry = SessionRegistry.getInstance();
        final var client = UnpluggedAfk.getInstance().getLinkClient();
        final var uuid = player.getUUID();
        final var name = player.getPlainTextName();

        if (!ack.accepted()) {
            LOGGER.warn("The proxy refused the unplug of {} ({}): {}", name, uuid, ack.reason());
            registry.clearUnplugging(uuid);
            player.getBukkitEntity().sendMessage(ChatMessages.formatUnplugRefused(ack.reason()));
            return;
        }

        if (player.hasDisconnected() || player.isDeadOrDying()) {
            LOGGER.warn(
                    "{} ({}) was no longer eligible when the proxy answered, so the session is undone.", name, uuid);
            client.endSession(uuid, END_ABORTED);
            registry.clearUnplugging(uuid);
            return;
        }

        try {
            commit(player, session);
        } catch (UnplugFailedException exception) {
            LOGGER.error("Failed to unplug {} ({}) after the proxy acknowledged the session.", name, uuid, exception);
            client.endSession(uuid, END_ABORTED);
        }
    }

    private static void commit(ServerPlayer player, Session session) throws UnplugFailedException {
        final var registry = SessionRegistry.getInstance();
        final var uuid = player.getUUID();
        final var name = player.getPlainTextName();
        final var level = player.level();
        final var playerList = level.getServer().getPlayerList();
        final var message = ChatMessages.formatUnplugged(session.durationMins(), session.reason());
        final var oldConnection = player.connection.connection;
        final var clientInformation = player.clientInformation();
        final var gameProfile = player.gameProfile;
        final var channels = player.getBukkitEntity().getListeningPluginChannels();

        var spawnScheduled = false;

        try {
            registry.markCommitting(uuid);
            player.getBukkitEntity().kick(message, PlayerKickEvent.Cause.PLUGIN);

            if (playerList.getPlayer(uuid) != null) {
                LOGGER.warn(
                        "A plugin cancelled the kick of {} ({}), but the unplug is already committed. Forcing it.",
                        name,
                        uuid);
                forceDisconnect(player, message);

                if (playerList.getPlayer(uuid) != null) {
                    throw new PlayerStillConnectedException(uuid, name);
                }
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
        final var details = new DisconnectionDetails(PaperAdventure.asVanilla(message));

        player.connection.onDisconnect(details);
        player.connection.connection.disconnect(details);
    }
}
