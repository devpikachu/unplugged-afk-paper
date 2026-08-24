package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Ready(boolean accepted, String reason) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.READY;
    }

    public static Ready read(DataInput in) throws IOException {
        return new Ready(in.readBoolean(), in.readUTF());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(this.accepted);
        out.writeUTF(this.reason);
    }
}
