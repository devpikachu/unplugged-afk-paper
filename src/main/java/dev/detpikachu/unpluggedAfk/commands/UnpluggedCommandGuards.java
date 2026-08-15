package dev.detpikachu.unpluggedAfk.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandErrors.ERR_NOT_A_PLAYER;
import static dev.detpikachu.unpluggedAfk.commands.UnpluggedCommandErrors.errDurationTooLarge;

public final class UnpluggedCommandGuards {

    public static ServerPlayer requireExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(context.getSource().getExecutor() instanceof CraftPlayer craftPlayer)) {
            throw ERR_NOT_A_PLAYER.create();
        }

        return craftPlayer.getHandle();
    }

    public static int requireDuration(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        final var durationMins = context.getArgument(argument, int.class);

        if (durationMins > UnpluggedOptions.getInstance().getMaxDurationMins()) {
            throw errDurationTooLarge(durationMins).create();
        }

        return durationMins;
    }
}
