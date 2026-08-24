package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

@ApiStatus.Internal
public record SessionEnd(UUID uuid, String reason) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.SESSION_END;
    }

    public static SessionEnd read(DataInput in) throws IOException {
        final var uuid = new UUID(in.readLong(), in.readLong());
        final var reason = in.readUTF();

        return new SessionEnd(uuid, reason);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(this.uuid.getMostSignificantBits());
        out.writeLong(this.uuid.getLeastSignificantBits());
        out.writeUTF(this.reason);
    }
}
