package dev.detpikachu.unpluggedafk.velocity.compat.miniplaceholders;

import com.velocitypowered.api.proxy.Player;
import dev.detpikachu.unpluggedafk.common.formatting.DurationFormatting;
import dev.detpikachu.unpluggedafk.velocity.BuildConstants;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.session.Session;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.utils.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Internal
public final class MiniPlaceholdersExpansion {

    static final String IDENTIFIER = "unplugged-afk";

    private static final String IS_UNPLUGGED = "is_unplugged";
    private static final String DURATION_MINS = "duration_mins";
    private static final String REASON = "reason";
    private static final String STARTED = "started";
    private static final String EXPIRES = "expires";
    private static final String REMAINING_MINS = "remaining_mins";
    private static final String SERVER = "server";
    private static final String COUNT = "count";

    public static Expansion build(UnpluggedAfkVelocity plugin) {
        final var sessionStore = plugin.getSessionStore();

        return Expansion.builder(IDENTIFIER)
                .author(authorsOf(plugin))
                .version(BuildConstants.VERSION)
                .audiencePlaceholder(
                        Player.class,
                        IS_UNPLUGGED,
                        (player, queue, context) ->
                                tag(String.valueOf(sessionStore.find(player.getUniqueId()) != null)))
                .audiencePlaceholder(
                        Player.class,
                        DURATION_MINS,
                        (player, queue, context) ->
                                sessionTag(sessionStore, player, session -> String.valueOf(session.durationMins())))
                .audiencePlaceholder(
                        Player.class,
                        REASON,
                        (player, queue, context) -> sessionTag(sessionStore, player, Session::reason))
                .audiencePlaceholder(
                        Player.class,
                        STARTED,
                        (player, queue, context) -> sessionTag(
                                sessionStore, player, session -> DurationFormatting.format(session.elapsed())))
                .audiencePlaceholder(
                        Player.class,
                        EXPIRES,
                        (player, queue, context) -> sessionTag(
                                sessionStore, player, session -> DurationFormatting.format(session.remaining())))
                .audiencePlaceholder(
                        Player.class,
                        REMAINING_MINS,
                        (player, queue, context) -> sessionTag(
                                sessionStore,
                                player,
                                session -> String.valueOf(session.remaining().toMinutes())))
                .audiencePlaceholder(
                        Player.class,
                        SERVER,
                        (player, queue, context) -> sessionTag(sessionStore, player, Session::serverName))
                .globalPlaceholder(COUNT, (queue, context) -> tag(String.valueOf(sessionStore.count())))
                .build();
    }

    private static String authorsOf(UnpluggedAfkVelocity plugin) {
        return plugin.getProxyServer()
                .getPluginManager()
                .fromInstance(plugin)
                .map(container -> String.join(", ", container.getDescription().getAuthors()))
                .orElse("");
    }

    private static Tag sessionTag(SessionStore sessionStore, Player player, Function<Session, String> value) {
        final var session = sessionStore.find(player.getUniqueId());
        return session == null ? Tags.EMPTY_TAG : tag(value.apply(session));
    }

    private static Tag tag(String value) {
        return Tag.selfClosingInserting(Component.text(value));
    }
}
