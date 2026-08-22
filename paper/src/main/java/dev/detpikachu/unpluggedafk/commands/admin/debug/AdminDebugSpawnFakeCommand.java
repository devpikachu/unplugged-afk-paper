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

import static dev.detpikachu.unpluggedafk.commands.CommandGuards.*;

@ApiStatus.Internal
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

    private static int execute(CommandContext<CommandSourceStack> context, @Nullable String reason) throws CommandSyntaxException {
        final var executor = requireExecutor(context);
        final var durationMins = requireDuration(context, ARG_DURATION_MINS);

        final var effectiveReason = (reason == null || reason.isBlank())
                ? executor.getName().getString()
                : reason;

        requireCapacity();

        BotFactory.spawnFake(
                executor.level(),
                executor.position(),
                executor.getYRot(),
                executor.getXRot(),
                new Session(durationMins, effectiveReason, true)
        );

        return 1;
    }
}
