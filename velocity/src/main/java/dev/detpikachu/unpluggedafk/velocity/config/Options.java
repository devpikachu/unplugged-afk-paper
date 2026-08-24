package dev.detpikachu.unpluggedafk.velocity.config;

import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@ApiStatus.Internal
public final class Options extends OptionsBase {

    private static final Options INSTANCE = new Options();

    private static final String FILE_NAME = "config.yml";

    private LinkOptions link = new LinkOptions();

    public static Options getInstance() {
        return INSTANCE;
    }

    public LinkOptions getLink() {
        return this.link;
    }

    public static void deserialize(Path dataDirectory, Logger logger) {
        final var file = dataDirectory.resolve(FILE_NAME);
        final var link = LinkOptions.deserialize(read(file, logger), logger);

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

        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                new Yaml(dumperOptions).dump(link.serialize(), writer);
            }
        } catch (IOException exception) {
            logger.error("Could not write {}, so the generated secret will not survive a restart.", file, exception);
        }
    }
}
