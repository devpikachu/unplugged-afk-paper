package dev.detpikachu.unpluggedafk.commands.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedafk.formatting.UnpluggedChatFormatting;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;

import static dev.detpikachu.unpluggedafk.UnpluggedConstants.PERM_ADMIN_INFO;

public final class AdminInfoCommand {

    private static final String CMD_INFO = "info";

    private static final String ARG_PLAYER = "player";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_INFO);

        final var player = Commands.argument(ARG_PLAYER, ArgumentTypes.player());

        return root
                .requires(context -> context.getSender().hasPermission(PERM_ADMIN_INFO))
                .then(player.executes(AdminInfoCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var sender = context.getSource().getSender();
        final var playerResolver = context.getArgument(ARG_PLAYER, PlayerSelectorArgumentResolver.class);

        final var player = playerResolver.resolve(context.getSource()).getFirst();
        sender.sendMessage(UnpluggedChatFormatting.formatPlayer(player));

        return 1;
    }
}
