package dev.detpikachu.unpluggedafk;

import dev.detpikachu.unpluggedafk.formatting.DumpFormatting;
import dev.detpikachu.unpluggedafk.session.Session;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class DumpWriter {

    private static final String DUMPS_DIRECTORY = "dumps";
    private static final String DUMP_EXTENSION = ".txt";
    private static final String NAME_SEPARATOR = "_";
    private static final Pattern UNSAFE_NAME_PATTERN = Pattern.compile("[^A-Za-z0-9_.-]");

    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault());

    public static void write(Player player, Session session) {
        final var timestamp = Instant.now();

        try {
            final var directory = dumpsDirectory();

            Files.createDirectories(directory);
            Files.writeString(
                    directory.resolve(fileName(player.getName(), timestamp)),
                    DumpFormatting.formatDump(player, session, timestamp));
        } catch (Exception exception) {
            LOGGER.error("Failed to write unplug dump for {} ({})", player.getName(), player.getUniqueId(), exception);
        }
    }

    private static Path dumpsDirectory() {
        return UnpluggedAfk.getInstance().getDataPath().resolve(DUMPS_DIRECTORY);
    }

    private static String fileName(String playerName, Instant timestamp) {
        return FILE_NAME_FORMAT.format(timestamp)
                + NAME_SEPARATOR
                + UNSAFE_NAME_PATTERN.matcher(playerName).replaceAll(NAME_SEPARATOR)
                + DUMP_EXTENSION;
    }
}
