package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import dev.detpikachu.unpluggedafk.common.network.codec.MessageCodec;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

@ApiStatus.Internal
public record SessionStart(
        UUID uuid, String username, @Nullable Skin skin, long secondsRemaining) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.SESSION_START;
    }

    public static SessionStart read(DataInput in) throws IOException {
        final var uuid = MessageCodec.readUuid(in);
        final var username = in.readUTF();
        final var skin = Skin.read(in);
        final var secondsRemaining = in.readLong();

        return new SessionStart(uuid, username, skin, secondsRemaining);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        MessageCodec.writeUuid(this.uuid, out);
        out.writeUTF(this.username);

        if (this.skin != null) {
            this.skin.write(out);
        } else {
            out.writeBoolean(false);
        }

        out.writeLong(this.secondsRemaining);
    }

    public record Skin(String value, @Nullable String signature) {

        public static @Nullable Skin read(DataInput in) throws IOException {
            if (!in.readBoolean()) {
                return null;
            }

            final var value = in.readUTF();
            return new Skin(value, in.readBoolean() ? in.readUTF() : null);
        }

        public void write(DataOutput out) throws IOException {
            out.writeBoolean(true);
            out.writeUTF(this.value);

            if (this.signature != null) {
                out.writeBoolean(true);
                out.writeUTF(this.signature);
            } else {
                out.writeBoolean(false);
            }
        }
    }
}
