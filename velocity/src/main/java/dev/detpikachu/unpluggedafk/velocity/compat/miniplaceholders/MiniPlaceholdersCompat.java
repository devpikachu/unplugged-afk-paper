package dev.detpikachu.unpluggedafk.velocity.compat.miniplaceholders;

import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Publishes session state to MiniPlaceholders on the proxy.
 *
 * <p>Verified against MiniPlaceholders 3.1.0 for Velocity, sha256
 * {@code 35c2f7eafb2675bc7ac5deab4209bcb6b8f1b435b0d3cb1af358fde528776761}.
 *
 * <p>Resolution is against any Adventure audience rather than a Bukkit player, so this half answers off
 * {@code SessionStore} with no backend involved.
 *
 * <p>The two surfaces are deliberately not identical. This one adds {@code server}, since only the proxy knows which
 * backend holds the bot, and omits {@code is_fake}, since a {@code spawn-fake} bot is excluded from the link's sync by
 * design and therefore has no proxy-side session to answer for. {@code count} means network-wide here and backend-local
 * on Paper.
 *
 * <p>There is no teardown twin of {@link #register} the way there is on Paper. Velocity cannot reload or unload a
 * plugin, so the only thing that ends a registration is the JVM exiting.
 *
 * <p>Values are inserted through {@link MiniPlaceholdersExpansion} as literal components rather than parsed text,
 * because the session reason is whatever the player typed after {@code /unplug} and re-parsing it would let them
 * smuggle MiniMessage markup into anything that renders these tags.
 */
@ApiStatus.Internal
public final class MiniPlaceholdersCompat {

    private static final String PLUGIN_NAME = "miniplaceholders";

    public static void register(UnpluggedAfkVelocity plugin) {
        if (!plugin.getProxyServer().getPluginManager().isLoaded(PLUGIN_NAME)) {
            return;
        }

        MiniPlaceholdersExpansion.build(plugin).register();

        plugin.getLogger()
                .info("MiniPlaceholders detected. Registering an expansion so other plugins can read session state.");
    }
}
