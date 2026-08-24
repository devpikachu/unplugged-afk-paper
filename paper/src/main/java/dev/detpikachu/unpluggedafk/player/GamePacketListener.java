package dev.detpikachu.unpluggedafk.player;

import dev.detpikachu.unpluggedafk.KickReasons;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import io.papermc.paper.connection.DisconnectionReason;
import net.kyori.adventure.text.Component;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class GamePacketListener extends ServerGamePacketListenerImpl {

    private final UnpluggedConnection unpluggedConnection;
    private final UnpluggedServerPlayer bot;

    public GamePacketListener(
            MinecraftServer server,
            UnpluggedConnection connection,
            UnpluggedServerPlayer bot,
            CommonListenerCookie cookie) {
        super(server, connection, bot, cookie);
        this.unpluggedConnection = connection;
        this.bot = bot;
    }

    @Override
    public void disconnect(DisconnectionDetails details) {
        final var isDuplicateLogin = details.disconnectionReason()
                .flatMap(DisconnectionReason::game)
                .filter(cause -> cause == PlayerKickEvent.Cause.DUPLICATE_LOGIN)
                .isPresent();

        if (!isDuplicateLogin) {
            super.disconnect(details);
            return;
        }

        this.bot.deferredDisconnect(Component.text(KickReasons.RETURNED), Reason.PLAYER_RETURNED);
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        super.onDisconnect(details);
        this.unpluggedConnection.closeChannel();
    }
}
