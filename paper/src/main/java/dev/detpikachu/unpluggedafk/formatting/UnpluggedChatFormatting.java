package dev.detpikachu.unpluggedafk.formatting;

import dev.detpikachu.unpluggedafk.config.UnpluggedOptions;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class UnpluggedChatFormatting {

    public static Component formatPlayer(Player player) {
        if (((CraftPlayer) player).getHandle() instanceof UnpluggedServerPlayer unpluggedPlayer) {
            return formatPlayer(unpluggedPlayer);
        }

        final var playerName = text(player.getName(), GOLD);
        final var realPlayer = text(" is a real player.", WHITE);

        return playerName.append(realPlayer);
    }

    public static Component formatPlayer(UnpluggedServerPlayer unpluggedPlayer) {
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

    public static Component formatList(Collection<UnpluggedServerPlayer> unpluggedPlayers) {
        final var header = text("Unplugged players: ", WHITE)
                .append(text(unpluggedPlayers.size(), GOLD))
                .append(text("/", WHITE))
                .append(text(UnpluggedOptions.getInstance().getMaxUnpluggedPlayers(), GOLD));

        final var lines = new ArrayList<Component>();
        lines.add(header);

        unpluggedPlayers.stream()
                .sorted(Comparator.comparingLong(UnpluggedServerPlayer::getTimeoutAtMillis))
                .map(UnpluggedChatFormatting::formatListEntry)
                .forEach(lines::add);

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private static Component formatListEntry(UnpluggedServerPlayer unpluggedPlayer) {
        final var name = text(unpluggedPlayer.getName().getString(), GOLD);
        final var fakeMarker = unpluggedPlayer.isFake() ? text(" (fake)", RED) : Component.empty();
        final var expiresLabel = text(" - expires in ", GRAY);
        final var expiresValue = formatTimestamp(unpluggedPlayer.getTimeoutAtMillis());

        return name.append(fakeMarker).append(expiresLabel).append(expiresValue);
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
