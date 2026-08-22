package dev.detpikachu.unpluggedafk.network;

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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class UnpluggedConnection extends Connection {

    private static final SocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 65535);

    public UnpluggedConnection(MinecraftServer server, PacketFlow receiving) {
        super(receiving);

        final var channel = new EmbeddedChannel();
        Connection.configureSerialization(channel.pipeline(), receiving, false, null);
        this.channel = channel;
        this.configurePacketHandler(channel.pipeline());

        this.setupOutboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(server.registryAccess())));
    }

    @Override
    public void send(@NonNull Packet<?> packet, @Nullable ChannelFutureListener futureListener, boolean bl) {
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
    public void setListenerForServerboundHandshake(@NonNull PacketListener packetListener) {
        // No-op
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(@NonNull ProtocolInfo<T> protocolInfo, @NonNull T packetListener) {
        // No-op
    }

    @Override
    public void tick() {
        // No-op
    }

    @Override
    public @NonNull SocketAddress getRemoteAddress() {
        return ADDRESS;
    }

    public void closeChannel() {
        this.channel.close();
    }
}
