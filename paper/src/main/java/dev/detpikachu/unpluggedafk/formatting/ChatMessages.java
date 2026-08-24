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
            return formatBot(bot);
        }

        final var playerName = text(player.getName(), GOLD);
        final var realPlayer = text(" is a real player.", WHITE);

        return playerName.append(realPlayer);
    }

    public static Component formatUnplugged(int durationMins, String reason) {
        final var prefix = text("You have successfully unplugged. Your character will remain online for ");
        final var duration = formatDuration(Duration.ofMinutes(durationMins));
        final var suffix = text(". Reason: ");
        final var reasonComponent = text(reason);

        return prefix.append(duration).append(suffix).append(reasonComponent);
    }

    public static Component formatUnplugRefused(String reason) {
        return text("Your unplug request was refused. ").append(text(reason)).color(RED);
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
                .sorted(Comparator.comparing(bot -> bot.getSession().expiresAt()))
                .map(ChatMessages::formatListEntry)
                .forEach(lines::add);

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private static Component formatBot(UnpluggedServerPlayer bot) {
        final var session = bot.getSession();
        final var name = text(bot.getPlainTextName(), GOLD);
        final var header =
                text("Unplugged information for ", WHITE).append(name).append(text(":", WHITE));

        return Component.join(
                JoinConfiguration.newlines(),
                header,
                formatRow("Duration: ", formatDuration(Duration.ofMinutes(session.durationMins()))),
                formatRow("Reason: ", text(session.reason())),
                formatRow("Unplugged: ", formatDuration(session.elapsed()).append(text(" ago", GRAY))),
                formatRow("Expires: in ", formatDuration(session.remaining())),
                formatRow("Is Fake: ", session.isFake() ? text("true", GREEN) : text("false", RED)));
    }

    private static Component formatRow(String label, Component value) {
        return text(label, GRAY).append(value);
    }

    private static Component formatListHeader(int unplugged, int unplugging) {
        final var label = text("Unplugged players: ", WHITE);
        final var unpluggedValue = text(unplugged, GOLD);
        final var pendingMarker = formatPendingMarker(unplugging);
        final var separator = text("/", WHITE);
        final var capValue = text(Options.getInstance().getMaxUnpluggedPlayers(), GOLD);

        return label.append(unpluggedValue)
                .append(pendingMarker)
                .append(separator)
                .append(capValue);
    }

    private static Component formatPendingMarker(int unplugging) {
        if (unplugging < 1) {
            return Component.empty();
        }

        return text("(+", GRAY).append(text(unplugging, GOLD)).append(text(")", GRAY));
    }

    private static Component formatListEntry(UnpluggedServerPlayer bot) {
        final var session = bot.getSession();

        final var name = text(bot.getPlainTextName(), GOLD);
        final var fakeMarker = session.isFake() ? text(" (fake)", RED) : Component.empty();
        final var expiresLabel = text(" - expires in ", GRAY);
        final var expiresValue = formatDuration(session.remaining());

        return name.append(fakeMarker).append(expiresLabel).append(expiresValue);
    }

    private static Component formatDuration(Duration duration) {
        return text(DurationFormatting.format(duration), WHITE);
    }
}
