package dev.detpikachu.unpluggedafk.compat.cmi;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;

/**
 * Marks a bot AFK in CMI the moment it spawns, and keeps CMI's AFK state from outliving it.
 *
 * <p>CMI's automatic AFK only fires after its {@code AutoAfkIn} idle window, and only for a player holding
 * {@code cmi.command.afk.auto}, which a bot resolves through LuckPerms like anyone else. Neither wait nor gate means
 * anything for a player who has already left, so the bot is placed in manual AFK straight away through
 * {@code CMIUser.setAfk}, the same call CMI's own {@code /afk} makes. CMI's {@code Modules.yml} switch and its AFK
 * {@code DisabledWorlds} are honoured.
 *
 * <p>CMI's AFK auto-kick is cancelled for a bot, because its kick commands would end the session early.
 *
 * <p>A bot is denied {@code cmi.command.kick.bypass} for as long as it lives, so an admin can always end a session with
 * {@code /cmi kick}. Without that, a bot standing in for an operator or a staff member inherits their bypass and no
 * in-game player can kick it. The denial is a permission attachment, which LuckPerms turns into a transient node on the
 * player's user, so it is removed with the bot rather than left for the returning player.
 *
 * <p>The AFK entry is dropped when the bot is removed. CMI keys it on UUID and only clears it from a next-tick task
 * that requires the player to be online, so without this a player returning after their session expired would inherit
 * the bot's AFK timestamp, and with it an auto-kick due hours ago.
 */
@ApiStatus.Internal
public final class CmiCompat {

    private static final String PLUGIN_NAME = "CMI";

    public static void register(UnpluggedAfk plugin) {
        final var pluginManager = plugin.getServer().getPluginManager();

        if (!pluginManager.isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        pluginManager.registerEvents(new CmiListener(plugin), plugin);
        Log.info(
                "CMI detected. Bots will be marked AFK as soon as they spawn, exempt from AFK kicks, and kickable by admins.");
    }
}
