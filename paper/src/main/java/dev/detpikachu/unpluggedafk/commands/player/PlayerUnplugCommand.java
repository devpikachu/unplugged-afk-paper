package dev.detpikachu.unpluggedafk.commands.player;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedafk.Constants.Permissions;
import dev.detpikachu.unpluggedafk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedafk.session.Session;
import dev.detpikachu.unpluggedafk.session.UnplugService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_GENERIC;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_REASON_REQUIRED;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireCapacity;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireDuration;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireExecutor;

@ApiStatus.Internal
public final class PlayerUnplugCommand {

    private static final String CMD_UNPLUG = "unplug";

    private static final String ARG_DURATION_MINS = "durationMins";
    private static final String ARG_REASON = "reason";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_UNPLUG);

        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1));
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString());

        return root.requires(context -> context.getSender().hasPermission(Permissions.UNPLUG))
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

        requireCapacity();

        try {
            UnplugService.unplug(player, new Session(durationMins, reason, false));
        } catch (UnplugFailedException exception) {
            LOGGER.error("Failed to unplug player {} ({})", player.getName().getString(), player.getUUID(), exception);
            throw ERR_GENERIC.create();
        }

        return 1;
    }
}
