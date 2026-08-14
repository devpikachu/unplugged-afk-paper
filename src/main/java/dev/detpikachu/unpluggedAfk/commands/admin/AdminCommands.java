package dev.detpikachu.unpluggedAfk.commands.admin;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedAfk.commands.admin.debug.AdminDebugCommands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class AdminCommands {

    private final static String CMD_UNPLUGGED = "unplugged";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        var root = Commands.literal(CMD_UNPLUGGED);

        // TODO: Permissions
        root
                .requires(sender -> sender.getSender().isOp())
                .then(AdminDebugCommands.construct())
                .then(AdminStatsCommand.construct());

        return root.build();
    }
}
