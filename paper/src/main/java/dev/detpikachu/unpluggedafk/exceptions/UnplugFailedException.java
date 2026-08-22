package dev.detpikachu.unpluggedafk.exceptions;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class UnplugFailedException extends Exception {

    public UnplugFailedException(String message) {
        super(message);
    }
}
