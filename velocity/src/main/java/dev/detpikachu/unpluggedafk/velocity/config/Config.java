package dev.detpikachu.unpluggedafk.velocity.config;

import dev.detpikachu.unpluggedafk.common.config.ConfigLoader;
import dev.detpikachu.unpluggedafk.common.config.ConfigMigration;
import dev.detpikachu.unpluggedafk.common.logging.Log;
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
        final var file = dataDirectory.resolve(FILE_NAME);
        final var loaded = ConfigLoader.load(file, Options.class, Options.CURRENT_VERSION, MIGRATIONS);

        if (loaded == null) {
            return false;
        }

        if (loaded.getLink().getSecret().isBlank()) {
            loaded.getLink().mintSecret();

            if (!ConfigLoader.save(file, Options.class, loaded)) {
                Log.error("The generated link secret could not be saved to {}, so no backend could link.", file);
                return false;
            }

            Log.info("Generated a link secret in {}. Copy it into link.secret on every backend.", file);
        }

        current = loaded;
        return true;
    }
}
