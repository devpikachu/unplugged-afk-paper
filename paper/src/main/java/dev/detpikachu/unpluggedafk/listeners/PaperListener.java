package dev.detpikachu.unpluggedafk.listeners;

import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.PlayerSnapshot;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
                            bot.getBukkitEntity(), bot.toInfo(), reason != null ? reason : Reason.UNKNOWN)
                    .callEvent();

            return;
        }

        if (registry.isUnplugging(player.getUniqueId())) {
            event.quitMessage(ChatMessages.formatUnpluggedBroadcast(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitSnapshot(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var registry = SessionRegistry.getInstance();

        if (!registry.isUnplugging(player.getUniqueId()) || UnpluggedServerPlayer.from(player) != null) {
            return;
        }

        registry.putSnapshot(player.getUniqueId(), PlayerSnapshot.capture(player));
        logDebug("Captured a snapshot of {} ({}) for their bot.", player.getName(), player.getUniqueId());
    }
}
