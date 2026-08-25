package dev.detpikachu.unpluggedafk.common.network;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Protocol {

    public static final int VERSION = 2;
    public static final int LENGTH_FIELD_BYTES = 4;
    public static final int MAX_FRAME_BYTES = 2097152; // 2 MiB
    public static final int MAX_PAYLOAD_BYTES = 1048576; // 1 MiB - Minecraft's client-bound custom payload size cap
    public static final int HEARTBEAT_SECS = 20;
    public static final int IDLE_TIMEOUT_SECS = 60;
    public static final long GRACE_SECS = 300;
}
