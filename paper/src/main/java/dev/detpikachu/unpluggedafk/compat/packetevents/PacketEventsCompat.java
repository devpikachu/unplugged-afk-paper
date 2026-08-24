package dev.detpikachu.unpluggedafk.compat.packetevents;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

/**
 * Cancels the kick PacketEvents issues when it fails to inject into a bot's fake channel.
 *
 * <p>A bot is a full {@code PlayerList} member, so PacketEvents tries to inject into it on join. It normally skips fake
 * players by matching the channel class name, but ProtocolLib replaces {@code Connection.channel} with its own proxy
 * first, so that check misses and PacketEvents concludes it never injected and kicks the bot a tick later.
 *
 * <p>The match has to stay exactly this narrow. Cancelling {@code PlayerKickEvent} any wider breaks reconnection,
 * because {@code PlayerList} evicts a stale bot with {@code DUPLICATE_LOGIN_MESSAGE}, which is also a kick. Filtering
 * on {@code Cause} does not help either: this kick is a plain {@code Cause.PLUGIN}, so the message is the only thing
 * that identifies it.
 */
@ApiStatus.Internal
public final class PacketEventsCompat {

    private static final String PLUGIN_NAME = "packetevents";

    public static void register(UnpluggedAfk plugin) {
        final var pluginManager = plugin.getServer().getPluginManager();

        if (!pluginManager.isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        pluginManager.registerEvents(new PacketEventsListener(), plugin);
        LOGGER.info("PacketEvents detected. Preventing bot kicks due to injection errors thrown by PacketEvents.");
    }
}
