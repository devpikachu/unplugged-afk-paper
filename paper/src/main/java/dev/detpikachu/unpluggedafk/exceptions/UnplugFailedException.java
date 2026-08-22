package dev.detpikachu.unpluggedafk.exceptions;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class UnplugFailedException extends Exception {

    protected UnplugFailedException(String message) {
        super(message);
    }
}
