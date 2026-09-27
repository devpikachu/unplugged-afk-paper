package dev.detpikachu.unpluggedafk.common.config;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
final class Migrations {

    private static final String KEY_VERSION = "version";
    private static final int UNVERSIONED = 1;
    private static final int INVALID = -1;

    static boolean migrate(Path file, int currentVersion, List<ConfigMigration> steps) {
        final var document = read(file);

        if (document == null) {
            return true;
        }

        final var version = versionOf(document);

        if (version == INVALID) {
            Log.error(
                    "The version in {} is not a whole number of 1 or more, so the file cannot be upgraded. Put back the number the plugin wrote, or delete the file.",
                    file);
            return false;
        }

        if (version == currentVersion) {
            return true;
        }

        if (version > currentVersion) {
            Log.error(
                    "{} is version {}, written by a newer plugin than this one, which reads version {}. Restore the {}.v{}.bak that plugin left, if any, or delete the file.",
                    file,
                    version,
                    currentVersion,
                    file.getFileName(),
                    currentVersion);
            return false;
        }

        final var backup = file.resolveSibling(file.getFileName() + ".v" + version + ".bak");
        final var staged = file.resolveSibling(file.getFileName() + ".migrating");

        try {
            steps.stream()
                    .filter(step -> step.from() >= version && step.from() < currentVersion)
                    .sorted(Comparator.comparingInt(ConfigMigration::from))
                    .forEach(step -> step.apply().accept(document));

            final var migrated = document.toMap();
            migrated.put(KEY_VERSION, currentVersion);

            if (!Files.exists(backup)) {
                Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }

            Files.writeString(staged, dump(migrated), StandardCharsets.UTF_8);
            Files.move(staged, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException exception) {
            Log.error("Could not migrate {} from version {} to {}.", file, version, currentVersion, exception);
            deleteQuietly(staged);
            return false;
        }

        Log.info(
                "Migrated {} from version {} to {}. The old file is kept as {}.",
                file,
                version,
                currentVersion,
                backup);
        return true;
    }

    private static @Nullable Document read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }

        try {
            final var raw = new Load(LoadSettings.builder().build())
                    .loadFromString(Files.readString(file, StandardCharsets.UTF_8));

            return raw instanceof Map<?, ?> map ? Document.of(map) : null;
        } catch (IOException | YamlEngineException exception) {
            return null;
        }
    }

    private static int versionOf(Document document) {
        final var version = document.get(KEY_VERSION);

        if (version == null) {
            return UNVERSIONED;
        }

        if (version instanceof Integer number && number >= 1) {
            return number;
        }

        return INVALID;
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            Log.warn("Could not delete {}. Delete it by hand.", file, exception);
        }
    }

    private static String dump(Object document) {
        return new Dump(DumpSettings.builder()
                        .setDefaultFlowStyle(FlowStyle.BLOCK)
                        .build())
                .dumpToString(document);
    }
}
