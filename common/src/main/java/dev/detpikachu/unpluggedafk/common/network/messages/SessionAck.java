package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageCodec;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

@ApiStatus.Internal
public record SessionAck(UUID uuid, boolean accepted, String reason) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.SESSION_ACK;
    }

    public static SessionAck read(DataInput in) throws IOException {
        final var uuid = MessageCodec.readUuid(in);
        final var accepted = in.readBoolean();
        final var reason = in.readUTF();

        return new SessionAck(uuid, accepted, reason);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        MessageCodec.writeUuid(this.uuid, out);
        out.writeBoolean(this.accepted);
        out.writeUTF(this.reason);
    }
}
