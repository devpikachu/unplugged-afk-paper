package dev.detpikachu.unpluggedAfk.commands;

import dev.detpikachu.unpluggedAfk.commands.admin.AdminCommands;
import dev.detpikachu.unpluggedAfk.commands.player.PlayerUnplugCommand;
import io.papermc.paper.command.brigadier.Commands;

public final class UnpluggedCommands {

    public static void register(Commands registrar) {
        registrar.register(AdminCommands.construct());
        registrar.register(PlayerUnplugCommand.construct());
    }
}
