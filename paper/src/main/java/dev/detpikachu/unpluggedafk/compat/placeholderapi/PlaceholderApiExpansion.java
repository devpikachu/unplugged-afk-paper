package dev.detpikachu.unpluggedafk.compat.placeholderapi;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.common.formatting.DurationFormatting;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@ApiStatus.Internal
public final class PlaceholderApiExpansion extends PlaceholderExpansion {

    private static final String IDENTIFIER = "unplugged-afk";

    private static final String IS_UNPLUGGED = "is_unplugged";
    private static final String DURATION_MINS = "duration_mins";
    private static final String REASON = "reason";
    private static final String STARTED = "started";
    private static final String EXPIRES = "expires";
    private static final String REMAINING_MINS = "remaining_mins";
    private static final String IS_FAKE = "is_fake";
    private static final String COUNT = "count";

    private final UnpluggedAfk plugin;

    public PlaceholderApiExpansion(UnpluggedAfk plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", this.plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, String params) {
        final var registry = SessionRegistry.getInstance();
        final var bot = player == null ? null : registry.find(player.getUniqueId());
        final var session = bot == null ? null : bot.getSession();

        return switch (params.toLowerCase(Locale.ROOT)) {
            case IS_UNPLUGGED -> player == null ? "" : String.valueOf(bot != null);
            case DURATION_MINS -> session == null ? "" : String.valueOf(session.durationMins());
            case REASON -> session == null ? "" : session.reason();
            case STARTED -> session == null ? "" : DurationFormatting.format(session.elapsed());
            case EXPIRES -> session == null ? "" : DurationFormatting.format(session.remaining());
            case REMAINING_MINS ->
                session == null ? "" : String.valueOf(session.remaining().toMinutes());
            case IS_FAKE -> session == null ? "" : String.valueOf(session.isFake());
            case COUNT -> String.valueOf(registry.count());
            default -> null;
        };
    }
}
