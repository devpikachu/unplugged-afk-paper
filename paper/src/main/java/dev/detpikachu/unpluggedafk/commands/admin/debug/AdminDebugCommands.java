package dev.detpikachu.unpluggedafk.commands.admin.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.detpikachu.unpluggedafk.Constants.Permissions;
import dev.detpikachu.unpluggedafk.config.Options;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class AdminDebugCommands {

    private static final String CMD_DEBUG = "debug";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_DEBUG);

        return root
                .requires(context -> Options.getInstance().isDebug()
                        && context.getSender().hasPermission(Permissions.ADMIN_DEBUG))
                .then(AdminDebugSpawnFakeCommand.construct());
    }
}
