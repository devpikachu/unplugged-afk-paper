package dev.detpikachu.unpluggedafk.common.messages;

import dev.detpikachu.unpluggedafk.common.Message;
import dev.detpikachu.unpluggedafk.common.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Ping(long id) implements Message {

    public static Ping read(DataInput in) throws IOException {
        return new Ping(in.readLong());
    }

    @Override
    public MessageType getType() {
        return MessageType.PING;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(this.id);
    }
}
