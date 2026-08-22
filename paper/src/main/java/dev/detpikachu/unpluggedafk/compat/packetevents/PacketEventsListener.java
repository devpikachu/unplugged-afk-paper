package dev.detpikachu.unpluggedafk.compat.packetevents;

import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

/**
 * Cancels the kick PacketEvents issues when it fails to inject into a bot's fake channel.
 *
 * <p>A bot is a full {@code PlayerList} member, so PacketEvents tries to inject into it on join. It normally skips
 * fake players by matching the channel class name, but ProtocolLib replaces {@code Connection.channel} with its own
 * proxy first, so that check misses and PacketEvents concludes it never injected and kicks the bot a tick later.
 *
 * <p>The match has to stay exactly this narrow. Cancelling {@code PlayerKickEvent} any wider breaks reconnection,
 * because {@code PlayerList} evicts a stale bot with {@code DUPLICATE_LOGIN_MESSAGE}, which is also a kick. Filtering
 * on {@code Cause} does not help either: this kick is a plain {@code Cause.PLUGIN}, so the message is the only thing
 * that identifies it.
 */
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
