package dev.detpikachu.unpluggedafk.network;

import com.google.common.io.ByteStreams;
import dev.detpikachu.unpluggedafk.Constants;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.session.Session;
import io.papermc.paper.configuration.GlobalConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class ProxyManager {

    public static void disconnectFromProxy(Player player, Component message) {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            return;
        }

        final var out = ByteStreams.newDataOutput();
        out.writeUTF(Constants.BungeeChannel.KICK_PLAYER_RAW);
        out.writeUTF(player.getName());
        out.writeUTF(GsonComponentSerializer.gson().serialize(message));

        player.sendPluginMessage(JavaPlugin.getPlugin(UnpluggedAfk.class), Constants.BungeeChannel.NAME, out.toByteArray());
    }

    public static void announceSessionToProxy(Player player, Session session) {
        if (!GlobalConfiguration.get().proxies.velocity.enabled) {
            UnpluggedAfk.logDebug("Proxy mode is off, so no SESSION_START was announced for {}.", player.getName());
            return;
        }

        final var out = ByteStreams.newDataOutput();
        out.writeUTF(Constants.SessionsChannel.SESSION_START);
        out.writeInt(session.durationMins());

        player.sendPluginMessage(JavaPlugin.getPlugin(UnpluggedAfk.class), Constants.SessionsChannel.NAME, out.toByteArray());
        LOGGER.info("Announced SESSION_START for {} ({}) to the proxy: {} minute(s).", player.getName(), player.getUniqueId(), session.durationMins());
    }
}
