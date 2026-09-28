package dev.detpikachu.unpluggedafk.compat.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class PacketEventsListener implements Listener {

    private static final String KICK_MESSAGE = "PacketEvents failed to inject into a channel";

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var bot = UnpluggedServerPlayer.from(event.getPlayer());

        if (bot == null) {
            return;
        }

        final var channel = (Channel) PacketEvents.getAPI().getPlayerManager().getChannel(event.getPlayer());
        final var pipeline = channel.pipeline();

        if (pipeline.get(PacketEvents.ENCODER_NAME) != null) {
            return;
        }

        pipeline.addLast(PacketEvents.ENCODER_NAME, new ChannelOutboundHandlerAdapter());
        Log.debug("Gave bot {} a PacketEvents encoder slot", bot.describe());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        final var bot = UnpluggedServerPlayer.from(event.getPlayer());

        if (bot == null) {
            return;
        }

        final var reason = PlainTextComponentSerializer.plainText().serialize(event.reason());

        if (!KICK_MESSAGE.equals(reason)) {
            return;
        }

        event.setCancelled(true);
        Log.debug("Refused a kick of bot {}: {}", bot.describe(), reason);
    }
}
