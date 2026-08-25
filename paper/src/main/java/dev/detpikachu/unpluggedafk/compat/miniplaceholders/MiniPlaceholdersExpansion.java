package dev.detpikachu.unpluggedafk.compat.miniplaceholders;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.common.formatting.DurationFormatting;
import dev.detpikachu.unpluggedafk.session.Session;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.utils.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.entity.Player;
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
    private static final String IS_FAKE = "is_fake";
    private static final String COUNT = "count";

    public static Expansion build(UnpluggedAfk plugin) {
        final var meta = plugin.getPluginMeta();

        return Expansion.builder(IDENTIFIER)
                .author(String.join(", ", meta.getAuthors()))
                .version(meta.getVersion())
                .audiencePlaceholder(
                        Player.class,
                        IS_UNPLUGGED,
                        (player, queue, context) ->
                                tag(String.valueOf(SessionRegistry.getInstance().find(player.getUniqueId()) != null)))
                .audiencePlaceholder(
                        Player.class,
                        DURATION_MINS,
                        (player, queue, context) ->
                                sessionTag(player, session -> String.valueOf(session.durationMins())))
                .audiencePlaceholder(
                        Player.class, REASON, (player, queue, context) -> sessionTag(player, Session::reason))
                .audiencePlaceholder(
                        Player.class,
                        STARTED,
                        (player, queue, context) ->
                                sessionTag(player, session -> DurationFormatting.format(session.elapsed())))
                .audiencePlaceholder(
                        Player.class,
                        EXPIRES,
                        (player, queue, context) ->
                                sessionTag(player, session -> DurationFormatting.format(session.remaining())))
                .audiencePlaceholder(
                        Player.class,
                        REMAINING_MINS,
                        (player, queue, context) -> sessionTag(
                                player,
                                session -> String.valueOf(session.remaining().toMinutes())))
                .audiencePlaceholder(
                        Player.class,
                        IS_FAKE,
                        (player, queue, context) -> sessionTag(player, session -> String.valueOf(session.isFake())))
                .globalPlaceholder(
                        COUNT,
                        (queue, context) ->
                                tag(String.valueOf(SessionRegistry.getInstance().count())))
                .build();
    }

    private static Tag sessionTag(Player player, Function<Session, String> value) {
        final var bot = SessionRegistry.getInstance().find(player.getUniqueId());
        return bot == null ? Tags.EMPTY_TAG : tag(value.apply(bot.getSession()));
    }

    private static Tag tag(String value) {
        return Tag.selfClosingInserting(Component.text(value));
    }
}
