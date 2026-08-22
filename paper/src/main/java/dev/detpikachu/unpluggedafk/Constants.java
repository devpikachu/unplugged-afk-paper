package dev.detpikachu.unpluggedafk;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Constants {

    public static final class Defaults {
        public static final boolean DEBUG = false;
        public static final int MAX_UNPLUGGED_PLAYERS = 16;
        public static final int MAX_DURATION_MINS = 480;
    }

    public static final class Permissions {
        public static final String UNPLUG = "unplugged-afk.unplug";
        public static final String ADMIN = "unplugged-afk.admin";
        public static final String ADMIN_INFO = "unplugged-afk.admin.info";
        public static final String ADMIN_LIST = "unplugged-afk.admin.list";
        public static final String ADMIN_DEBUG = "unplugged-afk.admin.debug";
    }

    public static final class KickReasons {
        public static final String DISABLED = "Plugin disabled";
        public static final String EXPIRED = "Expired";
        public static final String REMOVED = "Entity removed";
    }

    public static final class BungeeChannel {
        public static final String NAME = "BungeeCord";
        public static final String KICK_PLAYER_RAW = "KickPlayerRaw";
    }

    public static final class SessionsChannel {
        public static final String NAME = "unplugged-afk:sessions";
        public static final String SESSION_START = "SESSION_START";
    }

    public static final class PacketEventsCompat {
        public static final String KICK_MESSAGE = "PacketEvents failed to inject into a channel";
    }
}
