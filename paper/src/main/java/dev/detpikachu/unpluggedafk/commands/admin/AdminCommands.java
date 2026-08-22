package dev.detpikachu.unpluggedafk.commands.admin;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedafk.Constants.Permissions;
import dev.detpikachu.unpluggedafk.commands.admin.debug.AdminDebugCommands;
import dev.detpikachu.unpluggedafk.config.Options;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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

        return sender.hasPermission(Permissions.ADMIN)
                || sender.hasPermission(Permissions.ADMIN_INFO)
                || sender.hasPermission(Permissions.ADMIN_LIST)
                || (Options.getInstance().isDebug() && sender.hasPermission(Permissions.ADMIN_DEBUG));
    }
}
