package dev.detpikachu.unpluggedafk.player;

import dev.detpikachu.unpluggedafk.KickReasons;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.connection.DisconnectionReason;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;

import static net.kyori.adventure.text.Component.text;

@ApiStatus.Internal
public final class GamePacketListener extends ServerGamePacketListenerImpl {

    private final UnpluggedConnection unpluggedConnection;
    private final UnpluggedServerPlayer bot;
    private boolean isDisconnectProcessed;

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
        if (this.isDisconnectProcessed) {
            return;
        }

        final var isDuplicateLogin = details.disconnectionReason()
                .flatMap(DisconnectionReason::game)
                .filter(cause -> cause == PlayerKickEvent.Cause.DUPLICATE_LOGIN)
                .isPresent();

        if (!isDuplicateLogin) {
            super.disconnect(details);

            if (this.bot.quitReason == PlayerQuitEvent.QuitReason.KICKED) {
                this.bot.deferredDisconnect(PaperAdventure.asAdventure(details.reason()), null);
            }
            return;
        }

        this.bot.deferredDisconnect(text(KickReasons.RETURNED), Reason.PLAYER_RETURNED);
    }

    public boolean isDisconnectProcessed() {
        return this.isDisconnectProcessed;
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        if (this.isDisconnectProcessed) {
            return;
        }

        this.isDisconnectProcessed = true;
        super.onDisconnect(details);
        this.unpluggedConnection.closeChannel();
    }
}
