package dev.detpikachu.unpluggedafk.common.messages;

import dev.detpikachu.unpluggedafk.common.Message;
import dev.detpikachu.unpluggedafk.common.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public record Challenge(int protocolVersion, String nonce) implements Message {

    public static Challenge read(DataInput in) throws IOException {
        return new Challenge(in.readInt(), in.readUTF());
    }

    @Override
    public MessageType getType() {
        return MessageType.CHALLENGE;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.protocolVersion);
        out.writeUTF(this.nonce);
    }
}
