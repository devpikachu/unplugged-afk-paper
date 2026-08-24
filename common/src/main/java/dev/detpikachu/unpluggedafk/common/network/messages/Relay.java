package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import dev.detpikachu.unpluggedafk.common.network.Protocol;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

@ApiStatus.Internal
public final class Relay implements Message {

    private final UUID uuid;
    private final String channel;
    private final byte[] payload;

    public Relay(UUID uuid, String channel, byte[] payload) {
        this.uuid = uuid;
        this.channel = channel;
        this.payload = payload;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getChannel() {
        return this.channel;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    @Override
    public MessageType getType() {
        return MessageType.RELAY;
    }

    public static Relay read(DataInput in) throws IOException {
        final var uuid = new UUID(in.readLong(), in.readLong());
        final var channel = in.readUTF();
        final var length = in.readInt();

        if (length < 0 || length > Protocol.MAX_PAYLOAD_BYTES) {
            throw new IOException("Relay payload of " + length + " byte(s) is out of range.");
        }

        final var payload = new byte[length];
        in.readFully(payload);

        return new Relay(uuid, channel, payload);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(this.uuid.getMostSignificantBits());
        out.writeLong(this.uuid.getLeastSignificantBits());
        out.writeUTF(this.channel);
        out.writeInt(this.payload.length);
        out.write(this.payload);
    }
}
