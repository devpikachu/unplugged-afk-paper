package dev.detpikachu.unpluggedafk.common.messages;

import dev.detpikachu.unpluggedafk.common.Message;
import dev.detpikachu.unpluggedafk.common.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Goodbye(String reason) implements Message {

    public static Goodbye read(DataInput in) throws IOException {
        return new Goodbye(in.readUTF());
    }

    @Override
    public MessageType getType() {
        return MessageType.GOODBYE;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(this.reason);
    }
}
