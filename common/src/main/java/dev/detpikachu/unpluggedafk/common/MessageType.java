package dev.detpikachu.unpluggedafk.common;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public enum MessageType {

    CHALLENGE(0),
    AUTH(1),
    READY(2),
    SYNC(3),
    SESSION_START(4),
    SESSION_ACK(5),
    SESSION_END(6),
    GOODBYE(7),
    RELAY(8),
    PING(9),
    PONG(10);

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
