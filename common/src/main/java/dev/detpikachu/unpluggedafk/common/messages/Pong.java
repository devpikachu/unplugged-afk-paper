package dev.detpikachu.unpluggedafk.common.messages;

import dev.detpikachu.unpluggedafk.common.Message;
import dev.detpikachu.unpluggedafk.common.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Pong(long id) implements Message {

    public static Pong read(DataInput in) throws IOException {
        return new Pong(in.readLong());
    }

    @Override
    public MessageType getType() {
        return MessageType.PONG;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(this.id);
    }
}
