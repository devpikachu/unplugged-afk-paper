package dev.detpikachu.unpluggedafk.common.messages;

import dev.detpikachu.unpluggedafk.common.Message;
import dev.detpikachu.unpluggedafk.common.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Ready(boolean accepted, String reason) implements Message {

    public static Ready read(DataInput in) throws IOException {
        return new Ready(in.readBoolean(), in.readUTF());
    }

    @Override
    public MessageType getType() {
        return MessageType.READY;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(this.accepted);
        out.writeUTF(this.reason);
    }
}
