package dev.detpikachu.unpluggedafk.velocity;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Constants {

    public static final class Defaults {

        public static final String LINK_HOST = "0.0.0.0";
        public static final int LINK_PORT = 25580;
        public static final String LINK_SECRET = "";
        public static final int LINK_SECRET_BYTES = 32;
    }

    public static final class Link {

        public static final int HANDSHAKE_TIMEOUT_SECS = 10;
        public static final int ACCEPTOR_THREADS = 1;
        public static final int WORKER_THREADS = 2;
    }

    public static final class Sessions {

        public static final int GRACE_MINS = 5;
        public static final int RESYNC_SECS = 2;
        public static final int SWEEP_SECS = 30;
        public static final String FILE_NAME = "sessions.json";
    }

    public static final class SessionsChannel {

        public static final String NAME = "unplugged-afk:sessions";
        public static final String SESSION_START = "SESSION_START";

        public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from(NAME);
    }
}
