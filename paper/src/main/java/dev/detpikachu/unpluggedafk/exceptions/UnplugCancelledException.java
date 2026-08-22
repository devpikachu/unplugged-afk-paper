package dev.detpikachu.unpluggedafk.exceptions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public class UnplugCancelledException extends UnplugFailedException {

    private final transient @Nullable Component cancelMessage;

    public UnplugCancelledException(@Nullable Component cancelMessage) {
        super(describe(cancelMessage));
        this.cancelMessage = cancelMessage;
    }

    public @Nullable Component getCancelMessage() {
        return this.cancelMessage;
    }

    private static String describe(@Nullable Component cancelMessage) {
        if (cancelMessage == null) {
            return "Unplug cancelled with no reason given.";
        }

        return "Unplug cancelled: " + PlainTextComponentSerializer.plainText().serialize(cancelMessage);
    }
}
