package dev.detpikachu.unpluggedafk.common.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Configuration
public abstract class LinkOptionsBase {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 25580;
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    @Comment("The address of the link. The proxy listens on it and every backend dials it.")
    private String host = DEFAULT_HOST;

    @Comment("The port of the link, from 1 to 65535.")
    private int port = DEFAULT_PORT;

    @Comment("The shared link secret. The proxy generates it on first start. Copy it into every backend.")
    private String secret = "";

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getSecret() {
        return this.secret;
    }

    public boolean isValid() {
        return !this.host.isBlank() && !this.secret.isBlank();
    }

    protected void setSecret(String secret) {
        this.secret = secret;
    }

    protected void clampPort() {
        this.port = Clamps.inRange("link.port", this.port, DEFAULT_PORT, MIN_PORT, MAX_PORT);
    }

    protected void resetBlankHost() {
        if (!this.host.isBlank()) {
            return;
        }

        Log.warn("link.host is blank. Resetting to {}.", DEFAULT_HOST);
        this.host = DEFAULT_HOST;
    }
}
