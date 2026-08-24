package dev.detpikachu.unpluggedafk.common.network;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public enum MessageType {
    HEARTBEAT(0),
    CHALLENGE(1),
    AUTH(2),
    READY(3),
    GOODBYE(4),
    RELAY(5),
    SYNC(6),
    SESSION_START(7),
    SESSION_ACK(8),
    SESSION_END(9);

    private static final MessageType[] VALUES = values();

    private final int id;

    MessageType(int id) {
        this.id = id;
    }

    public static @Nullable MessageType byId(int id) {
        for (final var type : VALUES) {
            if (type.id == id) {
                return type;
            }
        }

        return null;
    }

    public int getId() {
        return id;
    }
}
