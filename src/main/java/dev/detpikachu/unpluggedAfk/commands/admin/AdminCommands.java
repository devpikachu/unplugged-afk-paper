package dev.detpikachu.unpluggedAfk.commands.admin;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedAfk.commands.admin.debug.AdminDebugCommands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_ADMIN;
import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_ADMIN_DEBUG;
import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_ADMIN_INFO;
import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_ADMIN_LIST;

public final class AdminCommands {

    private static final String CMD_UNPLUGGED = "unplugged";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_UNPLUGGED);

        return root
                .requires(AdminCommands::isAdmin)
                .then(AdminDebugCommands.construct())
                .then(AdminInfoCommand.construct())
                .then(AdminListCommand.construct())
                .build();
    }

    private static boolean isAdmin(CommandSourceStack stack) {
        final var sender = stack.getSender();

        return sender.hasPermission(PERM_ADMIN)
                || sender.hasPermission(PERM_ADMIN_INFO)
                || sender.hasPermission(PERM_ADMIN_LIST)
                || sender.hasPermission(PERM_ADMIN_DEBUG);
    }
}
