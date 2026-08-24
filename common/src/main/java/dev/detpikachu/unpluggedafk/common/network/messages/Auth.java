package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Auth(int protocolVersion, String serverName, String signature) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.AUTH;
    }

    public static Auth read(DataInput in) throws IOException {
        return new Auth(in.readInt(), in.readUTF(), in.readUTF());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.protocolVersion);
        out.writeUTF(this.serverName);
        out.writeUTF(this.signature);
    }
}
