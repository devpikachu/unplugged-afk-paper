package dev.detpikachu.unpluggedafk.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class LinkOptions extends OptionsBase {

    public static final String DEFAULT_HOST = "";
    public static final int DEFAULT_PORT = 25580;
    public static final String DEFAULT_SECRET = "";
    public static final String DEFAULT_SERVER_NAME = "";

    private static final String KEY_HOST = "link.host";
    private static final String KEY_PORT = "link.port";
    private static final String KEY_SECRET = "link.secret";
    private static final String KEY_SERVER_NAME = "link.serverName";

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private String host;
    private int port;
    private String secret;
    private String serverName;

    public LinkOptions() {
        this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
        this.secret = DEFAULT_SECRET;
        this.serverName = DEFAULT_SERVER_NAME;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getSecret() {
        return this.secret;
    }

    public String getServerName() {
        return this.serverName;
    }

    public void deserialize(FileConfiguration config) {
        this.host = config.getString(KEY_HOST, DEFAULT_HOST);
        this.port = config.getInt(KEY_PORT, DEFAULT_PORT);
        this.secret = config.getString(KEY_SECRET, DEFAULT_SECRET);
        this.serverName = config.getString(KEY_SERVER_NAME, DEFAULT_SERVER_NAME);
    }

    public boolean isValid() {
        return !this.host.isBlank()
                && !this.secret.isBlank()
                && !this.serverName.isBlank()
                && this.port >= MIN_PORT
                && this.port <= MAX_PORT;
    }
}
