package dev.detpikachu.unpluggedafk.player;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record UnpluggedSession(int durationMins, String reason, boolean isFake) {
}
