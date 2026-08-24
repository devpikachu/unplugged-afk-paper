package dev.detpikachu.unpluggedafk.network;

import com.google.common.io.ByteStreams;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.session.Session;
import io.papermc.paper.configuration.GlobalConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class ProxyManager {

    public static final String BUNGEE_CHANNEL_NAME = "BungeeCord";
    public static final String BUNGEE_KICK_PLAYER_RAW = "KickPlayerRaw";
    public static final String SESSIONS_CHANNEL_NAME = "unplugged-afk:sessions";
    public static final String SESSIONS_SESSION_START = "SESSION_START";

    public static void disconnectFromProxy(Player player, Component message) {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            return;
        }

        final var out = ByteStreams.newDataOutput();
        out.writeUTF(BUNGEE_KICK_PLAYER_RAW);
        out.writeUTF(player.getName());
        out.writeUTF(GsonComponentSerializer.gson().serialize(message));

        player.sendPluginMessage(UnpluggedAfk.getInstance(), BUNGEE_CHANNEL_NAME, out.toByteArray());
    }

    public static void announceSessionToProxy(Player player, Session session) {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            logDebug("Proxy mode is off, so no SESSION_START was announced for {}.", player.getName());
            return;
        }

        final var out = ByteStreams.newDataOutput();
        out.writeUTF(SESSIONS_SESSION_START);
        out.writeInt(session.durationMins());

        player.sendPluginMessage(UnpluggedAfk.getInstance(), SESSIONS_CHANNEL_NAME, out.toByteArray());
        LOGGER.info(
                "Announced SESSION_START for {} ({}) to the proxy: {} minute(s).",
                player.getName(),
                player.getUniqueId(),
                session.durationMins());
    }
}
