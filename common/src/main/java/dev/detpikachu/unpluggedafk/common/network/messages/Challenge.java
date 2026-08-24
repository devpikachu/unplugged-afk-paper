package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Challenge(int protocolVersion, String nonce) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.CHALLENGE;
    }

    public static Challenge read(DataInput in) throws IOException {
        return new Challenge(in.readInt(), in.readUTF());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.protocolVersion);
        out.writeUTF(this.nonce);
    }
}
