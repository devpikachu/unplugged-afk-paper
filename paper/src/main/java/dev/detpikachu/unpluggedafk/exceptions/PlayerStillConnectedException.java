package dev.detpikachu.unpluggedafk.exceptions;

import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

@ApiStatus.Internal
public final class PlayerStillConnectedException extends UnplugFailedException {

    public PlayerStillConnectedException(UUID uuid, String name) {
        super(name + " (" + uuid + ") was still connected after the kick, so nothing holds their spot.");
    }
}
