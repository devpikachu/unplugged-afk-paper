package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Hands each bot to TAB as an ordinary player.
 *
 * <p>Verified against TAB 6.2.0 for Velocity, sha256
 * {@code f94331947134242efa478b9b9bf04b29e37861721dee58c32b7ea57779aa735e}. Every call {@link TabBridge} makes is
 * reflective, because TAB publishes only {@code me.neznamy.tab.api} and nothing this needs lives there. A TAB release
 * that moves any of it therefore disables this compat rather than breaking the build.
 *
 * <p>TAB learns about players from Velocity's login events, which are never fired for a bot. So TAB would never format
 * a bot, sort it, resolve its placeholders or list it on other backends. The bot's fabricated proxy player is passed to
 * TAB exactly as TAB's own join listener passes a real one, and from then on TAB treats it as any other player,
 * TAB-Bridge placeholders included, since those ride the bot's relayed plugin messages.
 */
@ApiStatus.Internal
public final class TabCompat {

    private static final String PLUGIN_NAME = "tab";

    public static @Nullable TabBridge register(UnpluggedAfkVelocity plugin) {
        if (!plugin.getProxyServer().getPluginManager().isLoaded(PLUGIN_NAME)) {
            return null;
        }

        final var bridge = TabBridge.resolve();
        if (bridge == null) {
            Log.warn("TAB detected, but its internals have changed. Bots will not be shown by TAB.");
            return null;
        }

        Log.info("TAB detected. Bots will be handed to TAB like real players.");
        return bridge;
    }
}
