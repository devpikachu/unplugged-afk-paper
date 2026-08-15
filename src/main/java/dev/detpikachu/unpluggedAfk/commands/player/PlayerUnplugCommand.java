package dev.detpikachu.unpluggedAfk.commands.player;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.detpikachu.unpluggedAfk.UnpluggedPlayerManager;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_UNPLUG;
import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandErrors.ERR_NOT_A_PLAYER;
import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandErrors.ERR_REASON_REQUIRED;

public final class PlayerUnplugCommand {

    private final static String CMD_UNPLUG = "unplug";

    private final static String ARG_DURATION_MINS = "durationMins";
    private final static String ARG_REASON = "reason";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_UNPLUG);

        final var durationMins = Commands.argument(ARG_DURATION_MINS, IntegerArgumentType.integer(1, UnpluggedOptions.getInstance().getMaxDurationMins()));
        final var reason = Commands.argument(ARG_REASON, StringArgumentType.greedyString());

        return root
                .requires(sender -> sender.getSender().hasPermission(PERM_UNPLUG))
                .then(durationMins.then(reason.executes(PlayerUnplugCommand::execute)))
                .build();
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        final var executor = context.getSource().getExecutor();

        if (executor == null) {
            throw ERR_NOT_A_PLAYER.create();
        }

        final var level = ((CraftWorld) executor.getWorld()).getHandle();
        final var durationMins = context.getArgument(ARG_DURATION_MINS, int.class);
        final var reason = context.getArgument(ARG_REASON, String.class);

        if (reason == null || reason.isBlank()) {
            throw ERR_REASON_REQUIRED.create();
        }

        if (!(executor instanceof CraftPlayer craftPlayer)) {
            throw ERR_NOT_A_PLAYER.create();
        }

        if (!(craftPlayer.getHandle() instanceof ServerPlayer player)) {
            throw ERR_NOT_A_PLAYER.create();
        }

        // TODO: Try-catch
        UnpluggedPlayerManager.getInstance().unplugPlayer(server, level, player, durationMins, reason);

        return 1;
    }
}
