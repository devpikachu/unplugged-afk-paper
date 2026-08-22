package dev.detpikachu.unpluggedafk.session;

import dev.detpikachu.unpluggedafk.DumpWriter;
import dev.detpikachu.unpluggedafk.api.events.PlayerUnplugEvent;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.exceptions.PlayerStillConnectedException;
import dev.detpikachu.unpluggedafk.exceptions.UnplugCancelledException;
import dev.detpikachu.unpluggedafk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import dev.detpikachu.unpluggedafk.network.ProxyManager;
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

    public static void unplug(ServerPlayer player, Session session) throws UnplugFailedException {
        final var registry = SessionRegistry.getInstance();
        final var uuid = player.getUUID();
        final var name = player.getPlainTextName();
        final var level = player.level();
        final var server = level.getServer();
        final var playerList = server.getPlayerList();
        final var message = ChatMessages.formatUnplugged(session.durationMins(), session.reason());
        final var oldConnection = player.connection.connection;
        final var clientInformation = player.clientInformation();
        final var gameProfile = player.gameProfile;
        final var event = new PlayerUnplugEvent(player.getBukkitEntity(), session.durationMins(), session.reason());

        if (!event.callEvent()) {
            LOGGER.info("Refused to unplug {} ({}): another plugin cancelled the request.", name, uuid);
            throw new UnplugCancelledException(event.getCancelMessage());
        }

        LOGGER.info("Unplugging {} ({}) for {} minute(s): {}", name, uuid, session.durationMins(), session.reason());
        if (Options.getInstance().isDebug()) {
            DumpWriter.write(player.getBukkitEntity(), session);
        }

        var spawnScheduled = false;

        try {
            registry.markUnplugging(uuid);
            ProxyManager.announceSessionToProxy(player.getBukkitEntity(), session);
            ProxyManager.disconnectFromProxy(player.getBukkitEntity(), message);
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

            BotFactory.spawnWhenSettled(level, gameProfile, clientInformation, session, oldConnection);
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
