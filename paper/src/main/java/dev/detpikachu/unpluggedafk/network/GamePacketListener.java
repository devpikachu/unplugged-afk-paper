package dev.detpikachu.unpluggedafk.network;

import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class GamePacketListener extends ServerGamePacketListenerImpl {

    private final UnpluggedConnection unpluggedConnection;

    public GamePacketListener(
            MinecraftServer server,
            UnpluggedConnection connection,
            UnpluggedServerPlayer bot,
            CommonListenerCookie cookie) {
        super(server, connection, bot, cookie);
        this.unpluggedConnection = connection;
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        super.onDisconnect(details);
        this.unpluggedConnection.closeChannel();
    }
}
