package dev.detpikachu.unpluggedafk.velocity.config;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class Options extends OptionsBase {

    private static final Options INSTANCE = new Options();

    private static final String FILE_NAME = "config.yml";
    private static final String KEY_DEBUG = "debug";
    private static final boolean DEFAULT_DEBUG = false;
    private static final String POSIX_VIEW = "posix";
    private static final String SECRET_FILE_PERMISSIONS = "rw-------";

    private boolean debug = DEFAULT_DEBUG;
    private LinkOptions link = new LinkOptions();

    public static Options getInstance() {
        return INSTANCE;
    }

    public static void deserialize(Path dataDirectory) {
        final var file = dataDirectory.resolve(FILE_NAME);
        final var values = read(file);
        final var link = LinkOptions.deserialize(values == null ? Map.of() : values);

        INSTANCE.debug = flag(values == null ? Map.of() : values, KEY_DEBUG, DEFAULT_DEBUG);

        if (!link.getSecret().isBlank()) {
            INSTANCE.link = link;
            return;
        }

        INSTANCE.link = link.withGeneratedSecret();

        if (values == null) {
            Log.error(
                    "{} could not be parsed, so it is left untouched and no backend will link. Fix or delete it.",
                    file);
            return;
        }

        write(file, INSTANCE.link);
        Log.info("Generated a link secret in {}. Copy it into link.secret on every backend.", file);
    }

    public boolean isDebug() {
        return this.debug;
    }

    public LinkOptions getLink() {
        return this.link;
    }

    private static @Nullable Map<?, ?> read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }

        try (var reader = Files.newBufferedReader(file)) {
            final var loaded = new Yaml().load(reader);
            return loaded instanceof Map<?, ?> values ? values : Map.of();
        } catch (IOException | YAMLException exception) {
            Log.warn("Could not read {}.", file, exception);
            return null;
        }
    }

    private static void write(Path file, LinkOptions link) {
        final var dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        final var document = new LinkedHashMap<String, Object>();
        document.put(KEY_DEBUG, INSTANCE.debug);
        document.putAll(link.serialize());

        try {
            Files.createDirectories(file.getParent());
            createRestricted(file);

            try (var writer = Files.newBufferedWriter(file)) {
                new Yaml(dumperOptions).dump(document, writer);
            }

            restrictToOwner(file);
        } catch (IOException exception) {
            Log.error("Could not write {}, so the generated secret will not survive a restart.", file, exception);
        }
    }

    private static void createRestricted(Path file) {
        if (!file.getFileSystem().supportedFileAttributeViews().contains(POSIX_VIEW)) {
            return;
        }

        try {
            Files.createFile(
                    file,
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(SECRET_FILE_PERMISSIONS)));
        } catch (FileAlreadyExistsException exception) {
            Log.debug("{} already exists, so its permissions are repaired after the write instead.", file);
        } catch (IOException | UnsupportedOperationException exception) {
            Log.warn("Could not pre-create {} with restricted permissions. Check them by hand.", file, exception);
        }
    }

    private static void restrictToOwner(Path file) {
        if (!file.getFileSystem().supportedFileAttributeViews().contains(POSIX_VIEW)) {
            return;
        }

        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(SECRET_FILE_PERMISSIONS));
        } catch (IOException | UnsupportedOperationException exception) {
            Log.warn("Could not restrict the permissions of {}. Check them by hand.", file, exception);
        }
    }
}
