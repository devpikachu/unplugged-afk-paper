package dev.detpikachu.unpluggedafk.compat.husksync;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

/**
 * Repairs the snapshot HuskSync is about to apply across an unplug handoff.
 *
 * <p>HuskSync saves on quit and applies on join, both keyed on UUID alone, and its LOCKSTEP interlock only waits when
 * the user is checked out on a DIFFERENT server. An unplug is a quit and a join on one server, so nothing waits and the
 * read beats the competing write by around 20ms. That happens twice per session, once in each direction: the bot's join
 * races the player's quit-save, and the returning player's join races the bot's. Either one applies a snapshot exactly
 * one handoff out of date.
 *
 * <p>Both are repaired by handing HuskSync the data it should have read. The bot is repaired from itself, because our
 * own NBT load already beat the store. The returning player is repaired from a snapshot of the bot captured at its
 * teardown, kept only for a {@code Reason.PLAYER_RETURNED} removal because that is the one teardown the returning login
 * itself caused.
 *
 * <p>Zero health is withheld and re-applied rather than passed through. HuskSync applies health through the Bukkit API,
 * and {@code CraftLivingEntity.setHealth} turns a zero into a full {@code die()} with a generic damage source, so a
 * player returning to a snapshot taken from a dead bot is killed a second time. The clamp keeps that branch unreached
 * and {@link HuskSyncListener#onSyncComplete} then writes the zero through NMS, which never calls {@code die()}.
 *
 * <p>Cancelling the event is NOT the fix. The callback that runs on a non-cancelled event is what reaches
 * {@code completeSync}, and that is what unlocks the player, so a cancel strands the bot in HuskSync's locked set and
 * its teardown then refuses to save with {@code disconnected while locked - data will NOT be saved!}.
 */
@ApiStatus.Internal
public final class HuskSyncCompat {

    private static final String PLUGIN_NAME = "HuskSync";

    public static void register(UnpluggedAfk plugin) {
        final var pluginManager = plugin.getServer().getPluginManager();

        if (!pluginManager.isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        pluginManager.registerEvents(new HuskSyncListener(), plugin);
        LOGGER.info(
                "HuskSync detected. Snapshots will be patched so that the inventory gets correctly transferred between player and bot, and vice-versa.");
    }
}
