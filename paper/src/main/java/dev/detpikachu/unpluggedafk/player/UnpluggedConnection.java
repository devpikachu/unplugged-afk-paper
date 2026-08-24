package dev.detpikachu.unpluggedafk.player;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.network.LinkClient;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import java.util.UUID;

@ApiStatus.Internal
public final class UnpluggedConnection extends Connection {

    private static final SocketAddress ADDRESS = new InetSocketAddress(InetAddress.getLoopbackAddress(), 65535);
    private static final String BUNGEE_NAMESPACE = "bungeecord";
    private static final Set<String> BUNGEE_SUBCHANNELS = Set.of("GetServers", "PlayerCount");

    private final UUID uuid;
    private final LinkClient linkClient;

    public UnpluggedConnection(MinecraftServer server, PacketFlow receiving, UUID uuid) {
        super(receiving);

        this.uuid = uuid;
        this.linkClient = UnpluggedAfk.getInstance().getLinkClient();

        final var channel = new EmbeddedChannel();
        Connection.configureSerialization(channel.pipeline(), receiving, false, null);
        this.channel = channel;
        this.configurePacketHandler(channel.pipeline());

        this.setupOutboundProtocol(
                GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(server.registryAccess())));
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener futureListener, boolean bl) {
        // Everything else stays dropped. A plugin message is the only packet with an identity to relay.
        if (packet instanceof ClientboundCustomPayloadPacket(DiscardedPayload(Identifier id, byte[] data))
                && isRelayable(id, data)) {
            this.linkClient.relay(this.uuid, id.toString(), data);
        }
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

    private static boolean isRelayable(Identifier id, byte[] data) {
        final var namespace = id.getNamespace();

        if (BUNGEE_NAMESPACE.equals(namespace)) {
            return BUNGEE_SUBCHANNELS.contains(subchannelOf(data));
        }

        return !Identifier.DEFAULT_NAMESPACE.equals(namespace);
    }

    private static String subchannelOf(byte[] data) {
        try (var stream = new DataInputStream(new ByteArrayInputStream(data))) {
            return stream.readUTF();
        } catch (IOException exception) {
            return "";
        }
    }
}
