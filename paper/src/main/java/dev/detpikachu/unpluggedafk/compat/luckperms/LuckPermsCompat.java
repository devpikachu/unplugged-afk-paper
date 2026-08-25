package dev.detpikachu.unpluggedafk.compat.luckperms;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

/**
 * Attaches LuckPerms to a bot, so it resolves permissions the way the player it stands in for does.
 *
 * <p>LuckPerms injects its own permissible from a {@code PlayerLoginEvent} handler and has no {@code PlayerJoinEvent}
 * one. A bot is placed straight into the {@code PlayerList}, so it fires neither pre-login nor login, gets no injection
 * and no warning, and falls back to the vanilla permissible: operator status, which it inherits through the shared
 * {@code GameProfile}, plus {@code plugin.yml} defaults. Everything granted through a LuckPerms group is lost, an
 * AFK-kick bypass included.
 *
 * <p>The repair is LuckPerms' own login path minus the login. {@link LuckPermsBridge} builds its
 * {@code LuckPermsPermissible} for the bot and hands it to its {@code PermissibleInjector}, so no permission rule is
 * reimplemented here. Both classes live in the isolated loader LuckPerms bootstraps its real plugin from and are
 * unreachable by name, which is why the bridge resolves them off the classloader of the published API instance.
 *
 * <p>That permissible is also the bot's context handle: {@code BukkitContextManager} answers every query-options
 * question by asking the injected permissible, so without this every LuckPerms context lookup about a bot returns null.
 *
 * <p>Nothing un-injects. A bot's teardown reaches {@code PlayerList.remove} and so fires {@code PlayerQuitEvent}, which
 * LuckPerms already handles at MONITOR by un-injecting, and its own disable un-injects every online player.
 *
 * <p>Bots from {@code /unplugged debug spawn-fake} are skipped. They carry a random UUID LuckPerms has never seen, so
 * loading one would mint a storage record for a player who will never connect.
 */
@ApiStatus.Internal
public final class LuckPermsCompat {

    private static final String PLUGIN_NAME = "LuckPerms";

    public static void register(UnpluggedAfk plugin) {
        final var pluginManager = plugin.getServer().getPluginManager();

        if (!pluginManager.isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        final var bridge = LuckPermsBridge.resolve();

        if (bridge == null) {
            return;
        }

        pluginManager.registerEvents(new LuckPermsListener(bridge), plugin);
        LOGGER.info("LuckPerms detected. Bots will resolve permissions through it rather than operator status alone.");
    }
}
