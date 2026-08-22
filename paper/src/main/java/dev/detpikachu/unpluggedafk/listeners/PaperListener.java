package dev.detpikachu.unpluggedafk.listeners;

import dev.detpikachu.unpluggedafk.Constants.PacketEventsCompat;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class PaperListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (UnpluggedServerPlayer.from(event.getPlayer()) == null) {
            return;
        }

        event.joinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var registry = SessionRegistry.getInstance();
        final var bot = UnpluggedServerPlayer.from(player);

        if (bot != null) {
            final var reason = bot.getRemoveReason();

            registry.remove(bot);
            event.quitMessage(null);

            logDebug("Removed bot {} ({}). {} still active.", player.getName(), player.getUniqueId(), registry.count());

            new UnpluggedPlayerRemoveEvent(
                    bot.getBukkitEntity(),
                    bot.toInfo(),
                    reason != null ? reason : Reason.UNKNOWN).callEvent();

            return;
        }

        if (registry.isUnplugging(player.getUniqueId())) {
            event.quitMessage(ChatMessages.formatUnpluggedBroadcast(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        final var player = event.getPlayer();
        final var bot = UnpluggedServerPlayer.from(player);

        if (bot == null) {
            return;
        }

        final var reason = PlainTextComponentSerializer.plainText().serialize(event.reason());

        if (PacketEventsCompat.KICK_MESSAGE.equals(reason)) {
            event.setCancelled(true);
            logDebug("Refused a kick of bot {} ({}): {}", player.getName(), player.getUniqueId(), reason);
            return;
        }

        if (event.getCause() == PlayerKickEvent.Cause.DUPLICATE_LOGIN) {
            bot.setRemoveReason(Reason.PLAYER_RETURNED);
        }

        logDebug("Let a kick of bot {} ({}) through: {}", player.getName(), player.getUniqueId(), reason);
    }
}
