package dev.detpikachu.unpluggedafk;

public final class UnpluggedConstants {

    public static final boolean DEFAULT_DEBUG = false;
    public static final int DEFAULT_MAX_UNPLUGGED_PLAYERS = 16;
    public static final int DEFAULT_MAX_DURATION_MINS = 480;

    public static final int SPAWN_SETTLE_TICKS = 1;
    public static final int SPAWN_TIMEOUT_TICKS = 100;

    public static final String KICK_REASON_DISABLED = "Plugin disabled";
    public static final String KICK_REASON_EXPIRED = "Expired";
    public static final String KICK_REASON_REMOVED = "Entity removed";
    public static final String KICK_REASON_PACKETEVENTS = "PacketEvents failed to inject into a channel";

    public static final String PERM_UNPLUG = "unplugged-afk.unplug";
    public static final String PERM_ADMIN = "unplugged-afk.admin";
    public static final String PERM_ADMIN_INFO = "unplugged-afk.admin.info";
    public static final String PERM_ADMIN_LIST = "unplugged-afk.admin.list";
    public static final String PERM_ADMIN_DEBUG = "unplugged-afk.admin.debug";

    public static final String EXCEPTION_FAILED_TO_DISCONNECT = "Failed to disconnect player";

    public static final String CHANNEL_BUNGEE = "BungeeCord";
    public static final String SUBCHANNEL_KICK_PLAYER_RAW = "KickPlayerRaw";

    public static final String CHANNEL_SESSIONS = "unplugged-afk:sessions";
    public static final String MESSAGE_SESSION_START = "SESSION_START";
}
