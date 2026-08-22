package dev.detpikachu.unpluggedafk.api.events;

import dev.detpikachu.unpluggedafk.api.UnpluggedPlayerInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired once a bot has stopped holding a player's spot, however that came about.
 *
 * <p>{@link #getPlayer()} is the bot. The plugin has already forgotten it by this point, so
 * {@link dev.detpikachu.unpluggedafk.api.UnpluggedAfkApi#isUnplugged(java.util.UUID)} returns {@code false} and
 * {@link #getInfo()} is the only remaining description of the session.
 *
 * <p>The bot's playerdata has not been written yet; the server saves it immediately after this event returns. So
 * anything the bot is still carrying reaches the real player, and anything changed on it here is persisted too.
 *
 * <p>{@link #getReason()} separates the ordinary end of a session from a death or an eviction.
 *
 * <p>Also fired for throwaway bots from {@code /unplugged debug spawn-fake}.
 */
public final class UnpluggedPlayerRemoveEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UnpluggedPlayerInfo info;
    private final Reason reason;

    public UnpluggedPlayerRemoveEvent(Player player, UnpluggedPlayerInfo info, Reason reason) {
        super(player);
        this.info = info;
        this.reason = reason;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Returns a snapshot of the bot as it was when it stopped holding the spot.
     *
     * @return a snapshot of the bot as it was when it stopped holding the spot
     */
    public UnpluggedPlayerInfo getInfo() {
        return this.info;
    }

    /**
     * Returns what ended the session.
     *
     * @return what ended the session
     */
    public Reason getReason() {
        return this.reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Why a bot stopped holding a player's spot.
     */
    public enum Reason {

        /**
         * The declared window ran out. The ordinary end of a session.
         */
        EXPIRED,

        /**
         * The bot was killed. Its items dropped where it stood, and the player meets the death screen on returning.
         */
        DIED,

        /**
         * The bot was removed from the world without dying, such as being despawned by another plugin. A dimension
         * change does not count; the bot survives that.
         */
        ENTITY_REMOVED,

        /**
         * Unplugged AFK was disabled, usually because the server is stopping. Every bot is torn down at once and no
         * spot is held any longer.
         */
        PLUGIN_DISABLED,

        /**
         * The real player logged back in, so the server evicted their bot as a duplicate login. This is the session
         * ending the way it was meant to.
         */
        PLAYER_RETURNED,

        /**
         * The bot was created but could not be brought up, so it never held the spot. The player is disconnected and
         * nothing is holding their place.
         */
        SPAWN_FAILED,

        /**
         * Something outside Unplugged AFK removed the bot, such as another plugin kicking it.
         */
        UNKNOWN
    }
}
