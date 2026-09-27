package dev.detpikachu.unpluggedafk.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.detpikachu.unpluggedafk.Permissions;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import dev.detpikachu.unpluggedafk.config.Options;
import dev.detpikachu.unpluggedafk.session.SessionRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_ALREADY_UNPLUGGING;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_EXECUTOR_NOT_ALLOWED;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_NOT_A_PLAYER;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.ERR_REASON_NOT_PLAIN_TEXT;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.errCapReached;
import static dev.detpikachu.unpluggedafk.commands.CommandErrors.errDurationTooLarge;

@ApiStatus.Internal
public final class CommandGuards {

    public static final String ARG_DURATION_MINS = "durationMins";

    private static final String FORMATTING_CHARACTERS = "&<>\\§";

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
            Log.debug(
                    "{} asked for {} minute(s), over the {} minute cap.",
                    context.getSource().getSender().getName(),
                    durationMins,
                    maxDurationMins);
            throw errDurationTooLarge(durationMins).create();
        }

        return durationMins;
    }

    public static String requirePlainText(String reason) throws CommandSyntaxException {
        for (var index = 0; index < reason.length(); index++) {
            if (isPlainText(reason.charAt(index))) {
                continue;
            }

            Log.debug("Refused a reason carrying something other than plain text at index {}.", index);
            throw ERR_REASON_NOT_PLAIN_TEXT.create();
        }

        return reason;
    }

    public static void requireAllowedExecutor(ServerPlayer player) throws CommandSyntaxException {
        if (!player.getBukkitEntity().hasPermission(Permissions.UNPLUG)) {
            Log.debug(
                    "Refused an unplug for {} ({}): they lack the permission.",
                    player.getPlainTextName(),
                    player.getUUID());
            throw ERR_EXECUTOR_NOT_ALLOWED.create();
        }
    }

    public static void requireNotAlreadyUnplugging(ServerPlayer player) throws CommandSyntaxException {
        final var registry = SessionRegistry.getInstance();
        final var uuid = player.getUUID();

        if (registry.isUnplugging(uuid) || registry.isUnplugged(uuid)) {
            Log.debug(
                    "Refused a repeat unplug for {} ({}). One is already in flight.", player.getPlainTextName(), uuid);
            throw ERR_ALREADY_UNPLUGGING.create();
        }
    }

    public static void requireCapacity() throws CommandSyntaxException {
        final var maxUnpluggedPlayers = Options.getInstance().getMaxUnpluggedPlayers();

        if (SessionRegistry.getInstance().count() >= maxUnpluggedPlayers) {
            Log.warn(
                    "Refused an unplug request: all {} slot(s) are in use. Raise maxUnpluggedPlayers to allow more.",
                    maxUnpluggedPlayers);
            throw errCapReached().create();
        }
    }

    private static boolean isPlainText(char character) {
        return FORMATTING_CHARACTERS.indexOf(character) < 0
                && !Character.isISOControl(character)
                && Character.getType(character) != Character.FORMAT;
    }
}
