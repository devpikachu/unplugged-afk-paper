package dev.detpikachu.unpluggedafk.commands.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.detpikachu.unpluggedafk.Permissions;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class AdminListCommand {

    private static final String CMD_LIST = "list";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        return Commands.literal(CMD_LIST).requires(AdminListCommand::isAllowed).executes(AdminListCommand::execute);
    }

    private static boolean isAllowed(CommandSourceStack stack) {
        return stack.getSender().hasPermission(Permissions.ADMIN_LIST);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();

        sender.sendMessage(ChatMessages.formatList(SessionRegistry.getInstance().all()));

        return 1;
    }
}
