package dev.detpikachu.unpluggedAfk.commands.admin;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedAfk.commands.admin.debug.AdminDebugCommands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class AdminCommands {

    private final static String CMD_UNPLUGGED = "unplugged";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        var root = Commands.literal(CMD_UNPLUGGED);

        root.then(AdminDebugCommands.construct());

        return root.build();
    }
}
