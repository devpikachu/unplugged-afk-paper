package dev.detpikachu.unpluggedafk.commands.admin.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.detpikachu.unpluggedafk.Permissions;
import dev.detpikachu.unpluggedafk.config.Options;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class AdminDebugCommands {

    private static final String CMD_DEBUG = "debug";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        return Commands.literal(CMD_DEBUG)
                .requires(AdminDebugCommands::isDebug)
                .then(AdminDebugSpawnFakeCommand.construct());
    }

    private static boolean isDebug(CommandSourceStack stack) {
        return Options.getInstance().isDebug() && stack.getSender().hasPermission(Permissions.ADMIN_DEBUG);
    }
}
