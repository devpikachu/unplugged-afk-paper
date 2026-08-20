package dev.detpikachu.unpluggedafk.commands.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.detpikachu.unpluggedafk.UnpluggedPlayerManager;
import dev.detpikachu.unpluggedafk.formatting.UnpluggedChatFormatting;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import static dev.detpikachu.unpluggedafk.UnpluggedConstants.PERM_ADMIN_LIST;

public final class AdminListCommand {

    private static final String CMD_LIST = "list";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_LIST);

        return root
                .requires(context -> context.getSender().hasPermission(PERM_ADMIN_LIST))
                .executes(AdminListCommand::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        final var sender = context.getSource().getSender();

        sender.sendMessage(UnpluggedChatFormatting.formatList(UnpluggedPlayerManager.getInstance().getPlayers()));

        return 1;
    }
}
