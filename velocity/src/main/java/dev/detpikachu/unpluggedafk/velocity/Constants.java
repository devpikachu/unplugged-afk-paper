package dev.detpikachu.unpluggedafk.velocity;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Constants {

    public static final class Sessions {

        public static final int GRACE_MINS = 5;
        public static final String FILE_NAME = "sessions.json";
    }

    public static final class SessionsChannel {

        public static final String NAME = "unplugged-afk:sessions";
        public static final String SESSION_START = "SESSION_START";

        public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from(NAME);
    }
}
