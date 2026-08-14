package dev.detpikachu.unpluggedAfk.bookkeeping;

import dev.detpikachu.unpluggedAfk.UnpluggedConstants;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class UnpluggedEntry {

    private UnpluggedServerPlayer player;
    private UnpluggedStatus status = UnpluggedStatus.ACTIVE;
    private int durationMins = UnpluggedConstants.DEFAULT_DURATION;
    private long startAtMillis = -1L;
    private long timeoutAtMillis = -1L;
    private String reason = "";
    private @Nullable String outcome = null;

    public UnpluggedEntry(@Nonnull UnpluggedServerPlayer player) {
        this.player = player;
    }
}
