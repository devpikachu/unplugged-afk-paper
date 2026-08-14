package dev.detpikachu.unpluggedAfk.commands.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public final class AdminStatsCommand {

    private final static String CMD_STATS = "stats";

    private final static String ARG_PLAYER = "player";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_STATS);

        final var player = Commands.argument(ARG_PLAYER, ArgumentTypes.player());

        root.executes(AdminStatsCommand::execute).then(player.executes(AdminStatsCommand::executeWithPlayer));

        return root;
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        final var executor = context.getSource().getExecutor();
        final var level = ((CraftWorld) executor.getWorld()).getHandle();

        // TODO: Print global stats
        executor.sendPlainMessage("Global stats");
        return 1;
    }

    private static int executeWithPlayer(CommandContext<CommandSourceStack> context) {
        final var server = ((CraftServer) Bukkit.getServer()).getServer();
        final var executor = context.getSource().getExecutor();
        final var level = ((CraftWorld) executor.getWorld()).getHandle();

        final var playerResolver = context.getArgument(ARG_PLAYER, PlayerSelectorArgumentResolver.class);

        try {
            final var player = ((CraftPlayer) playerResolver.resolve(context.getSource()).getFirst()).getHandle();

            if (!(player instanceof UnpluggedServerPlayer unpluggedPlayer)) {
                // TODO: Send configurable message
                executor.sendPlainMessage("Not unplugged");
                return 1;
            }

            executor.sendPlainMessage("Duration: " + unpluggedPlayer.getDurationMins() + " | Reason: " + unpluggedPlayer.getReason());
        } catch (CommandSyntaxException e) {
            // TODO: Should swallow and print user-friendly error
            throw new RuntimeException(e);
        }

        return 1;
    }
}
