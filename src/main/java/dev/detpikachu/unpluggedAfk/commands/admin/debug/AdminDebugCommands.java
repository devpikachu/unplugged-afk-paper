package dev.detpikachu.unpluggedAfk.commands.admin.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class AdminDebugCommands {

    private final static String CMD_DEBUG = "debug";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        final var root = Commands.literal(CMD_DEBUG);

        root.then(AdminDebugSpawnFakeCommand.construct());

        return root;
    }
}
