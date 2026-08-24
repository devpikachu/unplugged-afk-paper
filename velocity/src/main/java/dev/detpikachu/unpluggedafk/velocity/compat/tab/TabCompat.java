package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Registers unplugged players with TAB so they appear in the network-wide tab list.
 *
 * <p>Verified against TAB 6.1.0 for Velocity, sha256
 * {@code 14fe9601ce09f2efe8a395ef2359dbf109a3572aa1b0b140b99c04b1b391d397}. Everything below was read out of that JAR,
 * and every call {@link TabBridge} makes is reflective, because TAB publishes only {@code me.neznamy.tab.api} and
 * nothing this needs lives there. A TAB release that moves any of it therefore disables this compat rather than
 * breaking the build.
 *
 * <p>A bot has no proxy connection and never can, so TAB never sees it as a {@code TabPlayer} and its global player
 * list, built from TAB's own roster, leaves it out. Players on the bot's own backend still see it, because that entry
 * comes from their own server and TAB does not prune entries it did not add. The gap is cross-backend only.
 *
 * <p>TAB already models a player it knows about but is not connected to: {@code ProxyPlayer}, its representation of
 * someone on another proxy. Handing one to {@code GlobalPlayerList.onJoin} is the entire feature, and TAB then owns
 * which viewers see it along with vanish, spy servers and server groups. None of that is reimplemented here.
 *
 * <p>{@code FeatureManager.getFeature("GlobalPlayerList")} returns null exactly when TAB is absent or its global player
 * list is off, since the feature is registered only when its configuration section is present. That single null check
 * is the whole presence gate, so on a network that does not already merge player lists across backends this does
 * nothing at all.
 *
 * <p>Never route through {@code FeatureManager.onJoin} instead. It dereferences the ProxySupport feature
 * unconditionally, so it throws whenever ProxySupport is not loaded, which is the usual configuration.
 */
@ApiStatus.Internal
public final class TabCompat {

    private static final String PLUGIN_NAME = "tab";

    public static final int SWEEP_FREQUENCY_SECS = 30;

    public static @Nullable TabBridge register(UnpluggedAfkVelocity plugin) {
        final var proxyServer = plugin.getProxyServer();
        final var logger = plugin.getLogger();

        if (!proxyServer.getPluginManager().isLoaded(PLUGIN_NAME)) {
            return null;
        }

        final var bridge = TabBridge.resolve(plugin);
        if (bridge == null) {
            logger.warn("TAB detected, but its internals have changed. Unplugged players stay backend-local.");
            return null;
        }

        proxyServer
                .getScheduler()
                .buildTask(plugin, () -> sweep(bridge, plugin.getSessionStore()))
                .repeat(Duration.ofSeconds(SWEEP_FREQUENCY_SECS))
                .schedule();

        proxyServer.getEventManager().register(plugin, new TabListener(bridge));

        logger.info("TAB detected. Bots will be synchronized across player lists in addition to real players.");
        return bridge;
    }

    private static void sweep(TabBridge bridge, SessionStore sessionStore) {
        for (final var uuid : bridge.trackedBots()) {
            if (!sessionStore.isAlive(uuid)) {
                bridge.removeBot(uuid);
            }
        }

        bridge.refresh();
    }
}
