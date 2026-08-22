package dev.detpikachu.unpluggedafk.commands.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.detpikachu.unpluggedafk.Constants.Permissions;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class AdminListCommand {

    private static final String CMD_LIST = "list";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_LIST);

        return root
                .requires(context -> context.getSender().hasPermission(Permissions.ADMIN_LIST))
                .executes(AdminListCommand::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();

        sender.sendMessage(ChatMessages.formatList(SessionRegistry.getInstance().all()));

        return 1;
    }
}
