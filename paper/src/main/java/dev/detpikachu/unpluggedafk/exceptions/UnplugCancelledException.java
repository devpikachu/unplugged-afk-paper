package dev.detpikachu.unpluggedafk.exceptions;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public class UnplugCancelledException extends UnplugFailedException {

    private final transient @Nullable Component cancelMessage;

    public UnplugCancelledException(String message, @Nullable Component cancelMessage) {
        super(message);
        this.cancelMessage = cancelMessage;
    }

    public @Nullable Component getCancelMessage() {
        return cancelMessage;
    }
}
