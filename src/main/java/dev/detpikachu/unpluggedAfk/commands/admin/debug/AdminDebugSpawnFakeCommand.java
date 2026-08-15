package dev.detpikachu.unpluggedAfk.commands.admin.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.detpikachu.unpluggedAfk.UnpluggedPlayerManager;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;

public final class AdminDebugSpawnFakeCommand {

    private final static String CMD_DUMMY = "spawn-fake";

    private final static String ARG_DURATION_MINS = "durationMins";
    private final static String ARG_REASON = "reason";

    private final static String DUMMY_UUID = "ccdb9503-cd7b-4ad2-b7cb-00d165a73e2a";
    private final static String DUMMY_NAME = "Fakeson";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_DUMMY);

        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1, UnpluggedOptions.getInstance().getMaxDurationMins()));
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString());

        root.then(durationMins.then(reason.executes(AdminDebugSpawnFakeCommand::executeWithReason)).executes(AdminDebugSpawnFakeCommand::execute));

        return root;
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        // TODO: If executor null, print error message stating command must be run by the player.
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        final var sender = context.getSource().getSender();
        final var executor = context.getSource().getExecutor();
        final var level = ((CraftWorld) executor.getWorld()).getHandle();

        final var durationMins = context.getArgument(ARG_DURATION_MINS, int.class);

        final var unpluggedPlayer = UnpluggedPlayerManager.getInstance().createFake(server, level, UUID.fromString(DUMMY_UUID), DUMMY_NAME, durationMins, sender.getName());
        unpluggedPlayer.connection.teleport(executor.getX(), executor.getY(), executor.getZ(), executor.getYaw(), executor.getPitch());

        return 1;
    }

    private static int executeWithReason(CommandContext<CommandSourceStack> context) {
        // TODO: If executor null, print error message stating command must be run by the player.
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        final var sender = context.getSource().getSender();
        final var executor = context.getSource().getExecutor();
        final var level = ((CraftWorld) executor.getWorld()).getHandle();

        final var durationMins = context.getArgument(ARG_DURATION_MINS, int.class);
        var reason = context.getArgument(ARG_REASON, String.class);

        if (reason == null || reason.isBlank()) {
            reason = sender.getName();
        }

        final var unpluggedPlayer = UnpluggedPlayerManager.getInstance().createFake(server, level, UUID.fromString(DUMMY_UUID), DUMMY_NAME, durationMins, reason);
        unpluggedPlayer.connection.teleport(executor.getX(), executor.getY(), executor.getZ(), executor.getYaw(), executor.getPitch());

        return 1;
    }
}
