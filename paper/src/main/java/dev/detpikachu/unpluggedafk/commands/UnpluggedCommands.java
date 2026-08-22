package dev.detpikachu.unpluggedafk.commands;

import dev.detpikachu.unpluggedafk.commands.admin.AdminCommands;
import dev.detpikachu.unpluggedafk.commands.player.PlayerUnplugCommand;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class UnpluggedCommands {

    public static void register(Commands registrar) {
        registrar.register(AdminCommands.construct());
        registrar.register(PlayerUnplugCommand.construct());
    }
}
