package dev.detpikachu.unpluggedafk.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only view of which players are currently unplugged.
 *
 * <p>An unplugged player is represented on the server by a bot holding their spot, and that bot carries the real
 * player's UUID. That UUID therefore keys every method here, so asking whether a Bukkit {@code Player} is a bot means
 * calling {@link #isUnplugged(UUID)} with {@code player.getUniqueId()}. A player and their own bot are never online at
 * the same time, so a UUID identifies at most one of the two.
 *
 * <p>Every method is safe to call from any thread. The backing collections are concurrent, so a call from an async task
 * will not throw or observe a partially updated map. Results are point-in-time snapshots and may be stale as soon as
 * they return.
 *
 * <p>Get an instance through Bukkit's services manager; see the package documentation.
 */
@ApiStatus.NonExtendable
public interface UnpluggedAfkApi {

    /**
     * Whether this player has been kicked by {@code /unplug} but their bot has not been placed yet.
     *
     * <p>The plugin waits for the kicked connection to finish closing before creating the bot, so this window is
     * normally a couple of ticks and lasts at most 100. It is disjoint from {@link #isUnplugged(UUID)}: no UUID is
     * reported by both at once, and a UUID in this state has no {@link UnpluggedPlayerInfo} yet.
     *
     * @param uuid the player's UUID
     * @return {@code true} if an unplug is in flight for this UUID
     */
    boolean isUnplugging(UUID uuid);

    /**
     * Whether a bot is currently standing in for this player.
     *
     * <p>This is false during the short window between the player being kicked and their bot being placed. Callers that
     * mean "leave this account alone" rather than "a bot exists" should check {@link #isUnplugging(UUID)} too.
     *
     * @param uuid the player's UUID
     * @return {@code true} if a bot exists for this UUID right now
     */
    boolean isUnplugged(UUID uuid);

    /**
     * Looks up the bot standing in for this player.
     *
     * @param uuid the player's UUID
     * @return a snapshot of the bot, or {@link Optional#empty()} if there is none, which is exactly when
     *     {@link #isUnplugged(UUID)} returns {@code false}
     */
    Optional<UnpluggedPlayerInfo> find(UUID uuid);

    /**
     * Every bot currently standing in for a player, including throwaway ones from {@code /unplugged debug spawn-fake}.
     *
     * @return an immutable snapshot in no particular order, empty if nobody is unplugged
     */
    List<UnpluggedPlayerInfo> all();
}
