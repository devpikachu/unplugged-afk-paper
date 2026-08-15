package dev.detpikachu.unpluggedAfk.formatting;

import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

// TODO: Configurable messages
public final class UnpluggedChatFormatting {

    public static Component format(CraftPlayer player) {
        if (player.getHandle() instanceof UnpluggedServerPlayer unpluggedPlayer) {
            return format(unpluggedPlayer);
        }

        final var playerName = text(player.getName(), GOLD);
        final var realPlayer = text(" is a real player.", WHITE);

        return playerName.append(realPlayer);
    }

    public static Component format(UnpluggedServerPlayer unpluggedPlayer) {
        final var informationFor = text("Unplugged information for ", WHITE);
        final var playerName = text(unpluggedPlayer.getName().getString(), GOLD);
        final var colon = text(":", WHITE);

        final var durationLabel = text("Duration: ", GRAY);
        final var durationValue = formatDuration(unpluggedPlayer.getDurationMins());

        final var startAtPrefix = text("Unplugged: ", GRAY);
        final var startAtValue = formatTimestamp(unpluggedPlayer.getStartAtMillis());
        final var startAtSuffix = text(" ago", GRAY);

        final var endsAtLabel = text("Expires: in ", GRAY);
        final var endsAtValue = formatTimestamp(unpluggedPlayer.getTimeoutAtMillis());

        final var reasonLabel = text("Reason: ", GRAY);
        final var reasonValue = text(unpluggedPlayer.getReason());

        final var isFakeLabel = text("Is Fake: ", GRAY);
        final var isFakeValue = unpluggedPlayer.isFake() ? text("true", GREEN) : text("false", RED);

        return informationFor.append(playerName).append(colon).appendNewline()
                .append(durationLabel).append(durationValue).appendNewline()
                .append(startAtPrefix).append(startAtValue).append(startAtSuffix).appendNewline()
                .append(endsAtLabel).append(endsAtValue).appendNewline()
                .append(reasonLabel).append(reasonValue).appendNewline()
                .append(isFakeLabel).append(isFakeValue);
    }

    public static Component formatUnplugged(int durationMins, String reason) {
        final var prefix = text("You have successfully unplugged. Your character will remain online for ");
        final var duration = formatDuration(durationMins);
        final var suffix = text(". Reason: ");
        final var reasonComponent = text(reason);

        return prefix.append(duration).append(suffix).append(reasonComponent);
    }

    public static Component formatUnpluggedBroadcast(Player player) {
        final var playerName = text(player.getName(), YELLOW);
        final var unplugged = text(" has unplugged, leaving their character behind", YELLOW);

        return playerName.append(unplugged);
    }

    private static Component formatDuration(int durationMins) {
        if (durationMins < 60) {
            return text(durationMins, WHITE).append(text(" minute(s)", WHITE));
        }

        final var durationHours = durationMins / 60;
        final var remainderDurationMins = durationMins % 60;
        return text(durationHours, WHITE)
                .append(text(" hour(s) ", WHITE))
                .append(text(remainderDurationMins, WHITE))
                .append(text(" minute(s)", WHITE));
    }

    private static Component formatTimestamp(long timestampMillis) {
        final var relativeMillis = Math.abs(System.currentTimeMillis() - timestampMillis);

        final var relativeSecs = relativeMillis / 1000L;
        if (relativeSecs < 60) {
            return text(relativeSecs, WHITE).append(text(" second(s)", WHITE));
        }

        final var relativeMins = relativeSecs / 60;
        if (relativeMins < 60) {
            return text(relativeMins, WHITE).append(text(" minute(s)", WHITE));
        }

        final var relativeHours = relativeMins / 60;
        final var remainderRelativeMins = relativeMins % 60;
        return text(relativeHours, WHITE)
                .append(text(" hour(s) ", WHITE))
                .append(text(remainderRelativeMins, WHITE))
                .append(text(" minute(s)", WHITE));
    }
}
