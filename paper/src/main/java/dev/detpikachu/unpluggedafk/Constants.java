package dev.detpikachu.unpluggedafk;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Constants {

    public static final class Defaults {

        public static final boolean DEBUG = false;
        public static final int MAX_UNPLUGGED_PLAYERS = 16;
        public static final int MAX_DURATION_MINS = 480;
        public static final String LINK_HOST = "";
        public static final int LINK_PORT = 25580;
        public static final String LINK_SECRET = "";
        public static final String LINK_SERVER_NAME = "";
    }

    public static final class Link {

        public static final int CONNECT_TIMEOUT_MS = 5000;
        public static final int HANDSHAKE_TIMEOUT_SECS = 10;
        public static final int WORKER_THREADS = 1;
        public static final int BACKOFF_MIN_SECS = 1;
        public static final int BACKOFF_MAX_SECS = 60;
        public static final int SHUTDOWN_WAIT_SECS = 1;
        public static final String GOODBYE_DISABLED = "Plugin disabled";
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
        public static final String SPAWN_FAILED = "Spawn failed";
        public static final String RETURNED = "Player returned";
    }

    public static final class BungeeChannel {

        public static final String NAME = "BungeeCord";
        public static final String KICK_PLAYER_RAW = "KickPlayerRaw";
    }

    public static final class SessionsChannel {

        public static final String NAME = "unplugged-afk:sessions";
        public static final String SESSION_START = "SESSION_START";
    }
}
