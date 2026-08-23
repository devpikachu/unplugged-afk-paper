package dev.detpikachu.unpluggedafk.compat.packetevents;

import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class PacketEventsListener implements Listener {

    private static final String KICK_MESSAGE = "PacketEvents failed to inject into a channel";

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        final var player = event.getPlayer();

        if (UnpluggedServerPlayer.from(player) == null) {
            return;
        }

        final var reason = PlainTextComponentSerializer.plainText().serialize(event.reason());

        if (!KICK_MESSAGE.equals(reason)) {
            return;
        }

        event.setCancelled(true);
        logDebug("Refused a kick of bot {} ({}): {}", player.getName(), player.getUniqueId(), reason);
    }
}
