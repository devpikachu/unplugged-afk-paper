package dev.detpikachu.unpluggedafk.compat.husksync;

import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import net.william278.husksync.api.BukkitHuskSyncAPI;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.event.BukkitPreSyncEvent;
import net.william278.husksync.event.BukkitSyncCompleteEvent;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import org.bukkit.craftbukkit.entity.CraftPlayer;
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

@ApiStatus.Internal
public final class HuskSyncListener implements Listener {

    private static final Duration HANDOFF_TTL = Duration.ofSeconds(30);
    private static final double WITHHELD_HEALTH = 1.0;

    private final ConcurrentHashMap<UUID, Handoff> handoffs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Instant> withheldDeaths = new ConcurrentHashMap<>();

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
            final var repaired = snapshotOf(user);

            event.editData(stale -> overwrite(stale, repaired));
            logDebug("Repaired HuskSync's snapshot for bot {} ({}).", bot.getPlainTextName(), bot.getUUID());

            return;
        }

        final var handoff = handoffs.remove(user.getUuid());
        final var repair = handoff == null || handoff.isExpired() ? null : handoff.data();
        final var incoming = repair == null ? unpack(event.getData()) : repair;
        final var withhold = incoming.getHealth().filter(health -> health.getHealth() <= 0.0).isPresent();

        if (repair == null && !withhold) {
            return;
        }

        event.editData(stale -> {
            if (repair != null) {
                overwrite(stale, repair);
            }

            if (withhold) {
                stale.getHealth().ifPresent(health -> health.setHealth(WITHHELD_HEALTH));
            }
        });

        if (repair != null) {
            logDebug("Repaired HuskSync's snapshot for {} ({}) with their bot's.", user.getName(), user.getUuid());
        }

        if (!withhold) {
            return;
        }

        withheldDeaths.values().removeIf(HuskSyncListener::isExpired);
        withheldDeaths.put(user.getUuid(), Instant.now());

        logDebug("Held back the death of {} ({}) until their sync completes.", user.getName(), user.getUuid());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSyncComplete(BukkitSyncCompleteEvent event) {
        final var user = (BukkitUser) event.getUser();
        final var withheldAt = withheldDeaths.remove(user.getUuid());

        if (withheldAt == null || isExpired(withheldAt) || user.hasDisconnected()) {
            return;
        }

        ((CraftPlayer) user.getPlayer()).getHandle().setHealth(0.0F);
        logDebug("Restored the death of {} ({}) now that their sync has completed.", user.getName(), user.getUuid());
    }

    private static void overwrite(DataSnapshot.Unpacked destination, DataSnapshot.Unpacked source) {
        source.getData().forEach(destination::setData);
    }

    private static DataSnapshot.Unpacked unpack(DataSnapshot.Packed packed) {
        return BukkitHuskSyncAPI.getInstance().unpackSnapshot(packed);
    }

    private static DataSnapshot.Unpacked snapshotOf(OnlineUser user) {
        return unpack(user.createSnapshot(DataSnapshot.SaveCause.API));
    }

    private static boolean isExpired(Instant capturedAt) {
        return Instant.now().isAfter(capturedAt.plus(HANDOFF_TTL));
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
            return HuskSyncListener.isExpired(capturedAt);
        }
    }
}
