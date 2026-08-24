package dev.detpikachu.unpluggedafk.common.network;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Protocol {

    public static final int VERSION = 1;
    public static final int LENGTH_FIELD_BYTES = 4;
    public static final int MAX_FRAME_BYTES = 2097152; // 2 MiB
}
