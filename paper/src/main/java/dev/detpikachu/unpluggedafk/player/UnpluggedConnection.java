package dev.detpikachu.unpluggedafk.player;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

@ApiStatus.Internal
public final class UnpluggedConnection extends Connection {

    private static final SocketAddress ADDRESS = new InetSocketAddress(InetAddress.getLoopbackAddress(), 65535);

    public UnpluggedConnection(MinecraftServer server, PacketFlow receiving) {
        super(receiving);

        final var channel = new EmbeddedChannel();
        Connection.configureSerialization(channel.pipeline(), receiving, false, null);
        this.channel = channel;
        this.configurePacketHandler(channel.pipeline());

        this.setupOutboundProtocol(
                GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(server.registryAccess())));
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener futureListener, boolean bl) {
        // No-op
    }

    @Override
    public void setReadOnly() {
        // No-op
    }

    @Override
    public void handleDisconnection() {
        this.closeChannel();
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener) {
        // No-op
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
        // No-op
    }

    @Override
    public void tick() {
        // No-op
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return ADDRESS;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void closeChannel() {
        this.channel.close();
    }
}
