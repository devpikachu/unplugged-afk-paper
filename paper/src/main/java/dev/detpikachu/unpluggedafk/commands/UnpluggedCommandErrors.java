package dev.detpikachu.unpluggedafk.commands;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.detpikachu.unpluggedafk.config.UnpluggedOptions;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import org.jetbrains.annotations.ApiStatus;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

@ApiStatus.Internal
public final class UnpluggedCommandErrors {

    public static final SimpleCommandExceptionType ERR_GENERIC = new SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(
                    text("An error occurred during the execution of the command. Please contact the server administrator.", RED)
            )
    );

    public static final SimpleCommandExceptionType ERR_NOT_A_PLAYER = new SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(
                    text("Command must be executed by or as a real player. Executing from console without /execute isn't supported. Executing as entities other than players is not supported.", RED)
            )
    );

    public static final SimpleCommandExceptionType ERR_REASON_REQUIRED = new SimpleCommandExceptionType(
            MessageComponentSerializer.message().serialize(
                    text("A reason must be given when unplugging.", RED)
            )
    );

    public static SimpleCommandExceptionType errDurationTooLarge(int durationMins) {
        return new SimpleCommandExceptionType(
                MessageComponentSerializer.message().serialize(
                        text("The duration of ", RED)
                                .append(text(durationMins, RED))
                                .append(text(" minute(s) is larger than the allowed maximum of ", RED))
                                .append(text(UnpluggedOptions.getInstance().getMaxDurationMins(), RED))
                                .append(text(" minute(s).", RED))
                )
        );
    }

    public static SimpleCommandExceptionType errCapReached() {
        return new SimpleCommandExceptionType(
                MessageComponentSerializer.message().serialize(
                        text("The server already has ", RED)
                                .append(text(UnpluggedOptions.getInstance().getMaxUnpluggedPlayers(), RED))
                                .append(text(" unplugged player(s). Please try again later.", RED))
                )
        );
    }
}
