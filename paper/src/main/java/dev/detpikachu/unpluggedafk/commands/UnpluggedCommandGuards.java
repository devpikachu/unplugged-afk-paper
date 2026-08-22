package dev.detpikachu.unpluggedafk.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.UnpluggedPlayerManager;
import dev.detpikachu.unpluggedafk.config.UnpluggedOptions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.commands.UnpluggedCommandErrors.*;

@ApiStatus.Internal
public final class UnpluggedCommandGuards {

    public static ServerPlayer requireExecutor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(context.getSource().getExecutor() instanceof CraftPlayer craftPlayer)) {
            throw ERR_NOT_A_PLAYER.create();
        }

        return craftPlayer.getHandle();
    }

    public static int requireDuration(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        final var durationMins = context.getArgument(argument, int.class);
        final var maxDurationMins = UnpluggedOptions.getInstance().getMaxDurationMins();

        if (durationMins > maxDurationMins) {
            UnpluggedAfk.logDebug("{} asked for {} minute(s), over the {} minute cap.", context.getSource().getSender().getName(), durationMins, maxDurationMins);
            throw errDurationTooLarge(durationMins).create();
        }

        return durationMins;
    }

    public static void requireCapacity() throws CommandSyntaxException {
        final var maxUnpluggedPlayers = UnpluggedOptions.getInstance().getMaxUnpluggedPlayers();

        if (UnpluggedPlayerManager.getInstance().count() >= maxUnpluggedPlayers) {
            UnpluggedAfk.LOGGER.warn("Refused an unplug request: all {} slot(s) are in use. Raise maxUnpluggedPlayers to allow more.", maxUnpluggedPlayers);
            throw errCapReached().create();
        }
    }
}
