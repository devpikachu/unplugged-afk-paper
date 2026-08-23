package dev.detpikachu.unpluggedafk.common;

import dev.detpikachu.unpluggedafk.common.messages.Auth;
import dev.detpikachu.unpluggedafk.common.messages.Challenge;
import dev.detpikachu.unpluggedafk.common.messages.Goodbye;
import dev.detpikachu.unpluggedafk.common.messages.Ping;
import dev.detpikachu.unpluggedafk.common.messages.Pong;
import dev.detpikachu.unpluggedafk.common.messages.Ready;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public final class MessageCodec {

    public static void write(Message message, DataOutput out) throws IOException {
        out.writeByte(message.getType().getId());
        message.write(out);
    }

    public static Message read(DataInput in) throws IOException {
        final var id = in.readUnsignedByte();
        final var type = MessageType.byId(id);

        if (type == null) {
            throw new IOException("Unknown message type ID " + id + ".");
        }

        return switch (type) {
            case CHALLENGE -> Challenge.read(in);
            case AUTH -> Auth.read(in);
            case READY -> Ready.read(in);
            case GOODBYE -> Goodbye.read(in);
            case PING -> Ping.read(in);
            case PONG -> Pong.read(in);
            case SYNC, SESSION_START, SESSION_ACK, SESSION_END, RELAY ->
                throw new IOException("Message type " + type + " is not supported by this version.");
        };
    }
}
