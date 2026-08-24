package dev.detpikachu.unpluggedafk.commands.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedafk.Permissions;
import dev.detpikachu.unpluggedafk.formatting.ChatMessages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class AdminInfoCommand {

    private static final String CMD_INFO = "info";

    private static final String ARG_PLAYER = "player";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var player = Commands.argument(ARG_PLAYER, ArgumentTypes.player()).executes(AdminInfoCommand::execute);

        return Commands.literal(CMD_INFO).requires(AdminInfoCommand::isAllowed).then(player);
    }

    private static boolean isAllowed(CommandSourceStack stack) {
        return stack.getSender().hasPermission(Permissions.ADMIN_INFO);
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var sender = context.getSource().getSender();
        final var playerResolver = context.getArgument(ARG_PLAYER, PlayerSelectorArgumentResolver.class);

        final var player = playerResolver.resolve(context.getSource()).getFirst();
        sender.sendMessage(ChatMessages.formatPlayer(player));

        return 1;
    }
}
