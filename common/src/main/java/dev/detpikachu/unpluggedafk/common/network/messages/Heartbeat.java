package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Heartbeat(long id) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.HEARTBEAT;
    }

    public static Heartbeat read(DataInput in) throws IOException {
        return new Heartbeat(in.readLong());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(this.id);
    }
}
