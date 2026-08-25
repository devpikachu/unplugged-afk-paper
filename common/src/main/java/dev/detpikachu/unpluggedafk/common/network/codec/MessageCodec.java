package dev.detpikachu.unpluggedafk.common.network.codec;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import dev.detpikachu.unpluggedafk.common.network.messages.Auth;
import dev.detpikachu.unpluggedafk.common.network.messages.Challenge;
import dev.detpikachu.unpluggedafk.common.network.messages.Goodbye;
import dev.detpikachu.unpluggedafk.common.network.messages.Heartbeat;
import dev.detpikachu.unpluggedafk.common.network.messages.Ready;
import dev.detpikachu.unpluggedafk.common.network.messages.Relay;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionAck;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionEnd;
import dev.detpikachu.unpluggedafk.common.network.messages.SessionStart;
import dev.detpikachu.unpluggedafk.common.network.messages.Sync;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

@ApiStatus.Internal
public final class MessageCodec {

    public static Message read(DataInput in) throws IOException {
        final var id = in.readUnsignedByte();
        final var type = MessageType.byId(id);

        if (type == null) {
            throw new IOException("Unknown message type ID " + id + ".");
        }

        return switch (type) {
            case HEARTBEAT -> Heartbeat.read(in);
            case CHALLENGE -> Challenge.read(in);
            case AUTH -> Auth.read(in);
            case READY -> Ready.read(in);
            case GOODBYE -> Goodbye.read(in);
            case RELAY -> Relay.read(in);
            case SYNC -> Sync.read(in);
            case SESSION_START -> SessionStart.read(in);
            case SESSION_ACK -> SessionAck.read(in);
            case SESSION_END -> SessionEnd.read(in);
        };
    }

    public static void write(Message message, DataOutput out) throws IOException {
        out.writeByte(message.getType().getId());
        message.write(out);
    }

    public static UUID readUuid(DataInput in) throws IOException {
        final var mostSignificantBits = in.readLong();
        final var leastSignificantBits = in.readLong();

        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    public static void writeUuid(UUID uuid, DataOutput out) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }
}
