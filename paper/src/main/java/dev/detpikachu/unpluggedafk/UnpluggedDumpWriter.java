package dev.detpikachu.unpluggedafk;

import dev.detpikachu.unpluggedafk.formatting.UnpluggedDumpFormatting;
import dev.detpikachu.unpluggedafk.player.UnpluggedSession;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class UnpluggedDumpWriter {

    private static final String DUMPS_DIRECTORY = "dumps";
    private static final String DUMP_EXTENSION = ".txt";
    private static final String NAME_SEPARATOR = "_";
    private static final Pattern UNSAFE_NAME_PATTERN = Pattern.compile("[^A-Za-z0-9_.-]");

    private static final DateTimeFormatter FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    public static void write(Player player, UnpluggedSession session) {
        final var timestamp = Instant.now();

        try {
            final var directory = dumpsDirectory();

            Files.createDirectories(directory);
            Files.writeString(
                    directory.resolve(fileName(player.getName(), timestamp)),
                    UnpluggedDumpFormatting.formatDump(player, session, timestamp));
        } catch (Exception exception) {
            UnpluggedAfk.LOGGER.error("Failed to write unplug dump for {} ({})", player.getName(), player.getUniqueId(), exception);
        }
    }

    private static Path dumpsDirectory() {
        return JavaPlugin.getPlugin(UnpluggedAfk.class).getDataPath().resolve(DUMPS_DIRECTORY);
    }

    private static String fileName(String playerName, Instant timestamp) {
        return FILE_NAME_FORMAT.format(timestamp)
                + NAME_SEPARATOR
                + UNSAFE_NAME_PATTERN.matcher(playerName).replaceAll(NAME_SEPARATOR)
                + DUMP_EXTENSION;
    }
}
