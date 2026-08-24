package dev.detpikachu.unpluggedafk.commands.player;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedafk.Permissions;
import dev.detpikachu.unpluggedafk.exceptions.UnplugCancelledException;
import dev.detpikachu.unpluggedafk.exceptions.UnplugFailedException;
import dev.detpikachu.unpluggedafk.session.Session;
import dev.detpikachu.unpluggedafk.session.UnplugService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_GENERIC;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_REASON_REQUIRED;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.errUnplugCancelled;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.ARG_DURATION_MINS;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireCapacity;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireDuration;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireExecutor;

@ApiStatus.Internal
public final class PlayerUnplugCommand {

    private static final String CMD_UNPLUG = "unplug";

    private static final String ARG_REASON = "reason";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1));
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString());

        return Commands.literal(CMD_UNPLUG)
                .requires(PlayerUnplugCommand::isAllowed)
                .then(durationMins.then(reason.executes(PlayerUnplugCommand::execute)))
                .build();
    }

    private static boolean isAllowed(CommandSourceStack stack) {
        return stack.getSender().hasPermission(Permissions.UNPLUG);
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = requireExecutor(context);
        final var durationMins = requireDuration(context);
        final var reason = context.getArgument(ARG_REASON, String.class);

        if (reason.isBlank()) {
            throw ERR_REASON_REQUIRED.create();
        }

        requireCapacity();

        try {
            UnplugService.unplug(player, new Session(durationMins, reason, false));
        } catch (UnplugCancelledException exception) {
            throw errUnplugCancelled(exception.getCancelMessage()).create();
        } catch (UnplugFailedException exception) {
            LOGGER.error("Failed to unplug player {} ({})", player.getPlainTextName(), player.getUUID(), exception);
            throw ERR_GENERIC.create();
        }

        return 1;
    }
}
