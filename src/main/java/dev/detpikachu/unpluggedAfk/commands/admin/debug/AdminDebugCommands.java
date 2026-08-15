package dev.detpikachu.unpluggedAfk.commands.admin.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_ADMIN;
import static dev.detpikachu.unpluggedAfk.UnpluggedConstants.PERM_ADMIN_DEBUG;

public final class AdminDebugCommands {

    private final static String CMD_DEBUG = "debug";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_DEBUG);

        return root
                .requires(context -> context.getSender().hasPermission(PERM_ADMIN_DEBUG))
                .then(AdminDebugSpawnFakeCommand.construct());
    }
}
