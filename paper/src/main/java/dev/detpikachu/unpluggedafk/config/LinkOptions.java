package dev.detpikachu.unpluggedafk.config;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record LinkOptions(String host, int port, String secret, String serverName) {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    public boolean isValid() {
        return !this.host.isBlank()
                && !this.secret.isBlank()
                && !this.serverName.isBlank()
                && this.port >= MIN_PORT
                && this.port <= MAX_PORT;
    }
}
