package dev.detpikachu.unpluggedAfk.commands;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

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
}
