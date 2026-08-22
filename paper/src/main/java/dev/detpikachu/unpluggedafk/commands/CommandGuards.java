package dev.detpikachu.unpluggedafk.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_NOT_A_PLAYER;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.errCapReached;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.errDurationTooLarge;

@ApiStatus.Internal
public final class CommandGuards {

    public static final String ARG_DURATION_MINS = "durationMins";

    public static ServerPlayer requireExecutor(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        if (!(context.getSource().getExecutor() instanceof CraftPlayer craftPlayer)) {
            throw ERR_NOT_A_PLAYER.create();
        }

        return craftPlayer.getHandle();
    }

    public static int requireDuration(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var durationMins = context.getArgument(ARG_DURATION_MINS, int.class);
        final var maxDurationMins = Options.getInstance().getMaxDurationMins();

        if (durationMins > maxDurationMins) {
            logDebug(
                    "{} asked for {} minute(s), over the {} minute cap.",
                    context.getSource().getSender().getName(),
                    durationMins,
                    maxDurationMins);
            throw errDurationTooLarge(durationMins).create();
        }

        return durationMins;
    }

    public static void requireCapacity() throws CommandSyntaxException {
        final var maxUnpluggedPlayers = Options.getInstance().getMaxUnpluggedPlayers();

        if (SessionRegistry.getInstance().count() >= maxUnpluggedPlayers) {
            LOGGER.warn(
                    "Refused an unplug request: all {} slot(s) are in use. Raise maxUnpluggedPlayers to allow more.",
                    maxUnpluggedPlayers);
            throw errCapReached().create();
        }
    }
}
