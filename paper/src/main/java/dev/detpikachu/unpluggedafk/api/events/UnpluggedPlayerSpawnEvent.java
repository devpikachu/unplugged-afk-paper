package dev.detpikachu.unpluggedafk.api.events;

import dev.detpikachu.unpluggedafk.api.UnpluggedPlayerInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired once a bot has been placed and is holding a player's spot.
 *
 * <p>{@link #getPlayer()} is the bot, not the player it stands in for. It is a real {@link Player} on the server
 * carrying that player's UUID, so by this point
 * {@link dev.detpikachu.unpluggedafk.api.UnpluggedAfkApi#isUnplugged(java.util.UUID)} already returns {@code true}
 * for it, and its inventory and position have been loaded.
 *
 * <p>The bot's own {@link org.bukkit.event.player.PlayerJoinEvent} has already fired by this point, and fires again
 * for the real player when they come back.
 *
 * <p>Also fired for throwaway bots from {@code /unplugged debug spawn-fake}. Check
 * {@link dev.detpikachu.unpluggedafk.api.UnpluggedPlayerInfo#isFake()} to tell those apart.
 */
public final class UnpluggedPlayerSpawnEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UnpluggedPlayerInfo info;

    public UnpluggedPlayerSpawnEvent(Player player, UnpluggedPlayerInfo info) {
        super(player);
        this.info = info;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * @return a snapshot of the bot as it stands now
     */
    public UnpluggedPlayerInfo getInfo() {
        return info;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
