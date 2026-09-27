package dev.detpikachu.unpluggedafk.config;

import dev.detpikachu.unpluggedafk.common.config.ConfigLoader;
import dev.detpikachu.unpluggedafk.common.config.ConfigMigration;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.List;

@ApiStatus.Internal
public final class Config {

    private static final String FILE_NAME = "config.yml";
    private static final List<ConfigMigration> MIGRATIONS = List.of();

    private static volatile Options current = new Options();

    public static Options get() {
        return current;
    }

    public static boolean load(Path dataDirectory) {
        final var loaded =
                ConfigLoader.load(dataDirectory.resolve(FILE_NAME), Options.class, Options.CURRENT_VERSION, MIGRATIONS);

        if (loaded == null) {
            return false;
        }

        current = loaded;
        return true;
    }
}
