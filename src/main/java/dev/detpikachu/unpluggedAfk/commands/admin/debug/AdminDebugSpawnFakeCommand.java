package dev.detpikachu.unpluggedAfk.commands.admin.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedAfk.UnpluggedPlayerManager;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;

import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandErrors.ERR_NOT_A_PLAYER;

public final class AdminDebugSpawnFakeCommand {

    private static final String CMD_SPAWN_FAKE = "spawn-fake";

    private static final String ARG_DURATION_MINS = "durationMins";
    private static final String ARG_REASON = "reason";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_SPAWN_FAKE);

        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1, UnpluggedOptions.getInstance().getMaxDurationMins()));
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString());

        return root.then(durationMins.then(reason.executes(AdminDebugSpawnFakeCommand::executeWithReason)).executes(AdminDebugSpawnFakeCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        final var sender = context.getSource().getSender();
        final var executor = context.getSource().getExecutor();

        if (executor == null) {
            throw ERR_NOT_A_PLAYER.create();
        }

        final var level = ((CraftWorld) executor.getWorld()).getHandle();
        final var durationMins = context.getArgument(ARG_DURATION_MINS, int.class);

        // TODO: Try-catch
        final var unpluggedPlayer = UnpluggedPlayerManager.getInstance().createFake(server, level, durationMins, sender.getName());
        unpluggedPlayer.connection.teleport(executor.getX(), executor.getY(), executor.getZ(), executor.getYaw(), executor.getPitch());

        return 1;
    }

    private static int executeWithReason(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        final var sender = context.getSource().getSender();
        final var executor = context.getSource().getExecutor();

        if (executor == null) {
            throw ERR_NOT_A_PLAYER.create();
        }

        final var level = ((CraftWorld) executor.getWorld()).getHandle();
        final var durationMins = context.getArgument(ARG_DURATION_MINS, int.class);
        var reason = context.getArgument(ARG_REASON, String.class);

        if (reason == null || reason.isBlank()) {
            reason = sender.getName();
        }

        // TODO: Try-catch
        final var unpluggedPlayer = UnpluggedPlayerManager.getInstance().createFake(server, level, durationMins, reason);
        unpluggedPlayer.connection.teleport(executor.getX(), executor.getY(), executor.getZ(), executor.getYaw(), executor.getPitch());

        return 1;
    }
}
