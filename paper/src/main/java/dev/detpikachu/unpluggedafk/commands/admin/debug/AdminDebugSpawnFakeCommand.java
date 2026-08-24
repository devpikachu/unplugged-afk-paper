package dev.detpikachu.unpluggedafk.commands.admin.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedafk.player.BotFactory;
import dev.detpikachu.unpluggedafk.session.Session;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import static dev.detpikachu.unpluggedafk.commands.CommandGuards.ARG_DURATION_MINS;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireCapacity;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireDuration;
import static dev.detpikachu.unpluggedafk.commands.CommandGuards.requireExecutor;

@ApiStatus.Internal
public final class AdminDebugSpawnFakeCommand {

    private static final String CMD_SPAWN_FAKE = "spawn-fake";

    private static final String ARG_REASON = "reason";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString())
                .executes(context -> execute(context, context.getArgument(ARG_REASON, String.class)));
        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1))
                .then(reason)
                .executes(context -> execute(context, null));

        return Commands.literal(CMD_SPAWN_FAKE).then(durationMins);
    }

    private static int execute(CommandContext<CommandSourceStack> context, @Nullable String reason)
            throws CommandSyntaxException {
        final var executor = requireExecutor(context);
        final var durationMins = requireDuration(context);

        requireCapacity();

        final var effectiveReason = (reason == null || reason.isBlank()) ? executor.getPlainTextName() : reason;

        BotFactory.spawnFake(
                executor.level(),
                executor.position(),
                executor.getYRot(),
                executor.getXRot(),
                new Session(durationMins, effectiveReason, true));

        return 1;
    }
}
