package dev.detpikachu.unpluggedafk.common.config;

import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@ApiStatus.Internal
public final class ConfigLoader {

    private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
            .charset(StandardCharsets.UTF_8)
            .build();

    public static <T> @Nullable T load(Path file, Class<T> type, int currentVersion, List<ConfigMigration> migrations) {
        if (!Migrations.migrate(file, currentVersion, migrations)) {
            return null;
        }

        final T loaded;

        try {
            loaded = YamlConfigurations.update(file, type, PROPERTIES);
        } catch (RuntimeException exception) {
            Log.error("{} could not be loaded, so the plugin will not start. Fix or delete it.", file, exception);
            return null;
        }

        return loaded;
    }

    public static <T> boolean save(Path file, Class<T> type, T config) {
        try {
            YamlConfigurations.save(file, type, config, PROPERTIES);
        } catch (RuntimeException exception) {
            Log.error("Could not write {}.", file, exception);
            return false;
        }

        return true;
    }
}
