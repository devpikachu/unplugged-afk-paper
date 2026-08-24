package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Goodbye(String reason) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.GOODBYE;
    }

    public static Goodbye read(DataInput in) throws IOException {
        return new Goodbye(in.readUTF());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(this.reason);
    }
}
