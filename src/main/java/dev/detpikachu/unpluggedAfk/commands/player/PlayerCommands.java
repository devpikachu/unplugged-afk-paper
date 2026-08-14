package dev.detpikachu.unpluggedAfk.commands.player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class PlayerCommands {

    private final static String CMD_UNPLUG = "unplug";

    private final static String ARG_DURATION = "duration";
    private final static String ARG_REASON = "reason";

    public static LiteralCommandNode<CommandSourceStack> construct() {
        var root = Commands.literal(CMD_UNPLUG).then(
                Commands.argument(ARG_DURATION, IntegerArgumentType.integer(1)).then(
                        Commands.argument(ARG_REASON, StringArgumentType.greedyString())
                                .executes(ctx -> {
                                            var duration = ctx.getArgument(ARG_DURATION, int.class);
                                            var reason = ctx.getArgument(ARG_REASON, String.class);
                                            var sender = ctx.getSource().getSender();

                                            sender.sendPlainMessage("Unplugging for " + duration + " minutes with reason " + reason);

                                            return Command.SINGLE_SUCCESS;
                                        }
                                )
                )
        );

        return root.build();
    }
}
