package dev.detpikachu.unpluggedafk.velocity.config;

import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class Options extends OptionsBase {

    private static final Options INSTANCE = new Options();

    private static final String FILE_NAME = "config.yml";
    private static final String KEY_DEBUG = "debug";
    private static final boolean DEFAULT_DEBUG = false;

    private boolean debug = DEFAULT_DEBUG;
    private LinkOptions link = new LinkOptions();

    public static Options getInstance() {
        return INSTANCE;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public LinkOptions getLink() {
        return this.link;
    }

    public static void deserialize(Path dataDirectory, Logger logger) {
        final var file = dataDirectory.resolve(FILE_NAME);
        final var values = read(file, logger);
        final var link = LinkOptions.deserialize(values, logger);

        INSTANCE.debug = flag(values, KEY_DEBUG, DEFAULT_DEBUG);

        if (!link.getSecret().isBlank()) {
            INSTANCE.link = link;
            return;
        }

        INSTANCE.link = link.withGeneratedSecret();
        write(file, INSTANCE.link, logger);
        logger.info("Generated a link secret in {}. Copy it into link.secret on every backend.", file);
    }

    private static Map<?, ?> read(Path file, Logger logger) {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }

        try (var reader = Files.newBufferedReader(file)) {
            final var loaded = new Yaml().load(reader);
            return loaded instanceof Map<?, ?> values ? values : Map.of();
        } catch (IOException | YAMLException exception) {
            logger.warn("Could not read {}, so the link falls back to its defaults.", file, exception);
            return Map.of();
        }
    }

    private static void write(Path file, LinkOptions link, Logger logger) {
        final var dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        final var document = new LinkedHashMap<String, Object>();
        document.put(KEY_DEBUG, INSTANCE.debug);
        document.putAll(link.serialize());

        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                new Yaml(dumperOptions).dump(document, writer);
            }
        } catch (IOException exception) {
            logger.error("Could not write {}, so the generated secret will not survive a restart.", file, exception);
        }
    }
}
