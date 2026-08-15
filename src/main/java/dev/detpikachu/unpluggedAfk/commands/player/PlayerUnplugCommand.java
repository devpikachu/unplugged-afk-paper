package dev.detpikachu.unpluggedAfk.commands.player;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedAfk.UnpluggedAfk;
import dev.detpikachu.unpluggedAfk.UnpluggedPlayerManager;
import dev.detpikachu.unpluggedAfk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedAfk.player.UnpluggedSession;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_UNPLUG;
import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandErrors.ERR_GENERIC;
import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandErrors.ERR_REASON_REQUIRED;
import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandGuards.requireDuration;
import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandGuards.requireExecutor;

public final class PlayerUnplugCommand {

    private static final String CMD_UNPLUG = "unplug";

    private static final String ARG_DURATION_MINS = "durationMins";
    private static final String ARG_REASON = "reason";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_UNPLUG);

        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1));
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString());

        return root
                .requires(context -> context.getSender().hasPermission(PERM_UNPLUG))
                .then(durationMins.then(reason.executes(PlayerUnplugCommand::execute)))
                .build();
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = requireExecutor(context);
        final var durationMins = requireDuration(context, ARG_DURATION_MINS);
        final var reason = context.getArgument(ARG_REASON, String.class);

        if (reason == null || reason.isBlank()) {
            throw ERR_REASON_REQUIRED.create();
        }

        try {
            UnpluggedPlayerManager.getInstance().createPlayer(player, new UnpluggedSession(durationMins, reason, false));
        } catch (UnplugFailedException exception) {
            UnpluggedAfk.LOGGER.error("Failed to unplug player {} ({})", player.getName(), player.getUUID(), exception);
            throw ERR_GENERIC.create();
        }

        return 1;
    }
}
