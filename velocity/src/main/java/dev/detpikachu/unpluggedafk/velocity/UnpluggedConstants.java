package dev.detpikachu.unpluggedafk.velocity;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class UnpluggedConstants {

    public static final int SESSION_GRACE_MINS = 5;
    public static final String SESSIONS_FILE_NAME = "sessions.json";

    public static final String CHANNEL_SESSIONS = "unplugged-afk:sessions";
    public static final String MESSAGE_SESSION_START = "SESSION_START";
}
