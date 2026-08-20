package dev.detpikachu.unpluggedafk.commands.admin.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.UnpluggedPlayerManager;
import dev.detpikachu.unpluggedafk.player.UnpluggedSession;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import static dev.detpikachu.unpluggedafk.commands.UnpluggedCommandGuards.requireCapacity;
import static dev.detpikachu.unpluggedafk.commands.UnpluggedCommandGuards.requireDuration;
import static dev.detpikachu.unpluggedafk.commands.UnpluggedCommandGuards.requireExecutor;

public final class AdminDebugSpawnFakeCommand {

    private static final String CMD_SPAWN_FAKE = "spawn-fake";

    private static final String ARG_DURATION_MINS = "durationMins";
    private static final String ARG_REASON = "reason";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_SPAWN_FAKE);

        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1));
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString());

        return root.then(durationMins
                .then(reason.executes(context -> execute(context, context.getArgument(ARG_REASON, String.class))))
                .executes(context -> execute(context, null)));
    }

    private static int execute(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        final var executor = requireExecutor(context);
        final var durationMins = requireDuration(context, ARG_DURATION_MINS);

        final var effectiveReason = (reason == null || reason.isBlank())
                ? executor.getName().getString()
                : reason;

        requireCapacity();

        final var unpluggedPlayer = UnpluggedPlayerManager.getInstance()
                .createFake(executor.level(), new UnpluggedSession(durationMins, effectiveReason, true));
        unpluggedPlayer.connection.teleport(executor.getX(), executor.getY(), executor.getZ(), executor.getYRot(), executor.getXRot());

        UnpluggedAfk.LOGGER.info(
                "{} spawned fake unplugged player {} at {}, {}, {} in {} for {} minute(s).",
                executor.getName().getString(),
                unpluggedPlayer.getName().getString(),
                (int) executor.getX(),
                (int) executor.getY(),
                (int) executor.getZ(),
                executor.level().dimension().identifier(),
                durationMins
        );

        return 1;
    }
}
