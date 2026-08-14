package dev.detpikachu.unpluggedAfk.commands.admin.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class AdminDebugDummyCommand {

    private final static String CMD_DUMMY = "dummy";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        var root = Commands.literal(CMD_DUMMY);

        return root;
    }
}
