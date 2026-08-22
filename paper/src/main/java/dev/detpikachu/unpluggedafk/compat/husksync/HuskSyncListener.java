package dev.detpikachu.unpluggedafk.compat.husksync;

import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import net.william278.husksync.api.BukkitHuskSyncAPI;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.event.BukkitPreSyncEvent;
import net.william278.husksync.user.OnlineUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

/**
 * Repairs the snapshot HuskSync is about to apply across an unplug handoff.
 *
 * <p>HuskSync saves on quit and applies on join, both keyed on UUID alone, and its LOCKSTEP interlock only waits when
 * the user is checked out on a DIFFERENT server. An unplug is a quit and a join on one server, so nothing waits and
 * the read beats the competing write by around 20ms. That happens twice per session, once in each direction: the
 * bot's join races the player's quit-save, and the returning player's join races the bot's. Either one applies a
 * snapshot exactly one handoff out of date.
 *
 * <p>Both are repaired by handing HuskSync the data it should have read. The bot is repaired from itself, because
 * our own NBT load already beat the store. The returning player is repaired from a snapshot of the bot captured at
 * its teardown, kept only for a {@link Reason#PLAYER_RETURNED} removal because that is the one teardown the
 * returning login itself caused.
 *
 * <p>Cancelling the event is NOT the fix. The callback that runs on a non-cancelled event is what reaches
 * {@code completeSync}, and that is what unlocks the player, so a cancel strands the bot in HuskSync's locked set and
 * its teardown then refuses to save with {@code disconnected while locked - data will NOT be saved!}.
 */
@ApiStatus.Internal
public final class HuskSyncListener implements Listener {

    private static final Duration HANDOFF_TTL = Duration.ofSeconds(30);

    private final ConcurrentHashMap<UUID, Handoff> handoffs = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var bot = UnpluggedServerPlayer.from(player);

        if (bot == null || bot.getRemoveReason() != Reason.PLAYER_RETURNED) {
            return;
        }

        handoffs.values().removeIf(Handoff::isExpired);
        handoffs.put(bot.getUUID(), new Handoff(snapshotOf(BukkitHuskSyncAPI.getInstance().getUser(player))));

        logDebug("Held the data of bot {} ({}) for the returning player.", bot.getPlainTextName(), bot.getUUID());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreSync(BukkitPreSyncEvent event) {
        final var user = event.getUser();
        final var bot = SessionRegistry.getInstance().find(user.getUuid());

        if (bot != null) {
            overwrite(event, snapshotOf(user));
            logDebug("Repaired HuskSync's snapshot for bot {} ({}).", bot.getPlainTextName(), bot.getUUID());

            return;
        }

        final var handoff = handoffs.remove(user.getUuid());

        if (handoff == null || handoff.isExpired()) {
            return;
        }

        overwrite(event, handoff.data());
        logDebug("Repaired HuskSync's snapshot for {} ({}) with their bot's.", user.getName(), user.getUuid());
    }

    private static void overwrite(BukkitPreSyncEvent event, DataSnapshot.Unpacked source) {
        event.editData(stale -> source.getData().forEach(stale::setData));
    }

    private static DataSnapshot.Unpacked snapshotOf(OnlineUser user) {
        return BukkitHuskSyncAPI.getInstance().unpackSnapshot(user.createSnapshot(DataSnapshot.SaveCause.API));
    }

    /**
     * A bot's data, held from its teardown until the returning player's sync reads it.
     *
     * <p>A handoff is normally consumed a few hundred milliseconds after it is captured. {@link #HANDOFF_TTL} only
     * backstops a login that fails before HuskSync gets to sync it, and comfortably exceeds HuskSync's own listen
     * timeout of sixteen attempts at ten ticks.
     */
    private record Handoff(DataSnapshot.Unpacked data, Instant capturedAt) {

        Handoff(DataSnapshot.Unpacked data) {
            this(data, Instant.now());
        }

        boolean isExpired() {
            return Instant.now().isAfter(capturedAt.plus(HANDOFF_TTL));
        }
    }
}
