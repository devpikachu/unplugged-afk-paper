package dev.detpikachu.unpluggedafk.common.network.messages;

import dev.detpikachu.unpluggedafk.common.network.Message;
import dev.detpikachu.unpluggedafk.common.network.MessageType;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public record Sync(List<SessionStart> sessions) implements Message {

    @Override
    public MessageType getType() {
        return MessageType.SYNC;
    }

    public static Sync read(DataInput in) throws IOException {
        final var count = in.readInt();

        if (count < 0) {
            throw new IOException("Sync of " + count + " session(s) is out of range.");
        }

        final var sessions = new ArrayList<SessionStart>();
        for (var i = 0; i < count; i++) {
            sessions.add(SessionStart.read(in));
        }

        return new Sync(sessions);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.sessions.size());

        for (final var session : this.sessions) {
            session.write(out);
        }
    }
}
