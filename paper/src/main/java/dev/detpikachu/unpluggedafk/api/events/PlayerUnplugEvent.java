package dev.detpikachu.unpluggedafk.api.events;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jspecify.annotations.Nullable;

/**
 * Fired when a player runs {@code /unplug}, before anything has happened.
 *
 * <p>Cancelling refuses the unplug: the player is not kicked, no bot is created, and nothing is announced to a proxy.
 * This is the only way for another plugin to veto an unplug, so it is where a combat-tag plugin refuses one mid-fight,
 * or a region plugin confines unplugging to a designated area.
 *
 * <p>The plugin's own checks have already passed by this point, so a cancellation is the last word.
 *
 * <p>{@link #getPlayer()} is the real player, still online. The bot does not exist yet, which is why this event
 * carries the requested duration and reason rather than an
 * {@link dev.detpikachu.unpluggedafk.api.UnpluggedPlayerInfo}: that record describes a bot that exists.
 *
 * <p>Not fired for {@code /unplugged debug spawn-fake}, which spawns a throwaway bot rather than unplugging anyone.
 */
public final class PlayerUnplugEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int durationMins;
    private final String reason;
    private boolean isCancelled = false;
    private @Nullable Component cancelMessage;

    public PlayerUnplugEvent(Player player, int durationMins, String reason) {
        super(player);
        this.durationMins = durationMins;
        this.reason = reason;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Returns how many minutes the player asked to stay unplugged.
     *
     * @return how many minutes the player asked to stay unplugged, already checked against the configured maximum
     */
    public int getDurationMins() {
        return this.durationMins;
    }

    /**
     * Returns the reason the player gave for unplugging.
     *
     * @return the reason the player gave, never blank
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * The message shown to the player if this event is canceled.
     *
     * @return the message, or {@code null} if a generic refusal should be used
     */
    public @Nullable Component getCancelMessage() {
        return this.cancelMessage;
    }

    /**
     * Sets the message shown to the player if this event is canceled. Set it alongside cancelling, otherwise the
     * player is told only that the request was refused.
     *
     * @param cancelMessage the message, or {@code null} for a generic refusal
     */
    public void setCancelMessage(@Nullable Component cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
