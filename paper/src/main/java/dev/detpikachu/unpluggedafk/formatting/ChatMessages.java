package dev.detpikachu.unpluggedafk.formatting;

import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

@ApiStatus.Internal
public final class ChatMessages {

    public static Component formatPlayer(Player player) {
        final var bot = UnpluggedServerPlayer.from(player);

        if (bot != null) {
            return formatPlayer(bot);
        }

        final var playerName = text(player.getName(), GOLD);
        final var realPlayer = text(" is a real player.", WHITE);

        return playerName.append(realPlayer);
    }

    public static Component formatPlayer(UnpluggedServerPlayer bot) {
        final var informationFor = text("Unplugged information for ", WHITE);
        final var playerName = text(bot.getName().getString(), GOLD);
        final var colon = text(":", WHITE);

        final var durationLabel = text("Duration: ", GRAY);
        final var durationValue = formatDuration(Duration.ofMinutes(bot.getDurationMins()));

        final var reasonLabel = text("Reason: ", GRAY);
        final var reasonValue = text(bot.getReason());

        final var startAtPrefix = text("Unplugged: ", GRAY);
        final var startAtValue = formatDuration(bot.elapsed());
        final var startAtSuffix = text(" ago", GRAY);

        final var endsAtLabel = text("Expires: in ", GRAY);
        final var endsAtValue = formatDuration(bot.remaining());

        final var isFakeLabel = text("Is Fake: ", GRAY);
        final var isFakeValue = bot.isFake() ? text("true", GREEN) : text("false", RED);

        return informationFor.append(playerName)
                .append(colon)
                .appendNewline()
                .append(durationLabel)
                .append(durationValue)
                .appendNewline()
                .append(reasonLabel)
                .append(reasonValue)
                .appendNewline()
                .append(startAtPrefix)
                .append(startAtValue)
                .append(startAtSuffix)
                .appendNewline()
                .append(endsAtLabel)
                .append(endsAtValue)
                .appendNewline()
                .append(isFakeLabel)
                .append(isFakeValue);
    }

    public static Component formatUnplugged(int durationMins, String reason) {
        final var prefix = text("You have successfully unplugged. Your character will remain online for ");
        final var duration = formatDuration(Duration.ofMinutes(durationMins));
        final var suffix = text(". Reason: ");
        final var reasonComponent = text(reason);

        return prefix.append(duration).append(suffix).append(reasonComponent);
    }

    public static Component formatUnpluggedBroadcast(Player player) {
        final var playerName = text(player.getName(), YELLOW);
        final var unplugged = text(" has unplugged, leaving their character behind", YELLOW);

        return playerName.append(unplugged);
    }

    public static Component formatList(Collection<UnpluggedServerPlayer> bots) {
        final var lines = new ArrayList<Component>();
        lines.add(formatListHeader(bots.size(), SessionRegistry.getInstance().countUnplugging()));

        bots.stream()
                .sorted(Comparator.comparing(UnpluggedServerPlayer::getExpiresAt))
                .map(ChatMessages::formatListEntry)
                .forEach(lines::add);

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private static Component formatListHeader(int unplugged, int unplugging) {
        final var label = text("Unplugged players: ", WHITE);
        final var unpluggedValue = text(unplugged, GOLD);
        final var pendingMarker = formatPendingMarker(unplugging);
        final var separator = text("/", WHITE);
        final var capValue = text(Options.getInstance().getMaxUnpluggedPlayers(), GOLD);

        return label.append(unpluggedValue).append(pendingMarker).append(separator).append(capValue);
    }

    private static Component formatPendingMarker(int unplugging) {
        if (unplugging < 1) {
            return Component.empty();
        }

        return text("(+", GRAY).append(text(unplugging, GOLD)).append(text(")", GRAY));
    }

    private static Component formatListEntry(UnpluggedServerPlayer bot) {
        final var name = text(bot.getName().getString(), GOLD);
        final var fakeMarker = bot.isFake() ? text(" (fake)", RED) : Component.empty();
        final var expiresLabel = text(" - expires in ", GRAY);
        final var expiresValue = formatDuration(bot.remaining());

        return name.append(fakeMarker).append(expiresLabel).append(expiresValue);
    }

    private static Component formatDuration(Duration duration) {
        final var seconds = duration.toSeconds();
        if (seconds < 60) {
            return text(seconds, WHITE).append(text(" second(s)", WHITE));
        }

        final var minutes = duration.toMinutes();
        if (minutes < 60) {
            return text(minutes, WHITE).append(text(" minute(s)", WHITE));
        }

        return text(duration.toHours(), WHITE).append(text(" hour(s) ", WHITE))
                .append(text(duration.toMinutesPart(), WHITE))
                .append(text(" minute(s)", WHITE));
    }
}
