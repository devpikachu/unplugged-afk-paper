package dev.detpikachu.unpluggedafk.network;

import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class UnpluggedGamePacketListener extends ServerGamePacketListenerImpl {

    private final UnpluggedConnection unpluggedConnection;

    public UnpluggedGamePacketListener(MinecraftServer server, UnpluggedConnection connection, UnpluggedServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
        this.unpluggedConnection = connection;
    }

    @Override
    public void onDisconnect(final DisconnectionDetails details) {
        super.onDisconnect(details);
        this.unpluggedConnection.closeChannel();
    }
}
