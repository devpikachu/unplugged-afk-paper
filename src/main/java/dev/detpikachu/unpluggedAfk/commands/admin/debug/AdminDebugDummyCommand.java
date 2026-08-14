package dev.detpikachu.unpluggedAfk.commands.admin.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.detpikachu.unpluggedAfk.UnpluggedPlayerManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;

public final class AdminDebugDummyCommand {

    private final static String CMD_DUMMY = "dummy";
    private final static String DUMMY_UUID = "ccdb9503-cd7b-4ad2-b7cb-00d165a73e2a";
    private final static String DUMMY_NAME = "Testison";

    public static LiteralArgumentBuilder<CommandSourceStack> construct() {
        var root = Commands.literal(CMD_DUMMY);

        root.executes(AdminDebugDummyCommand::execute);

        return root;
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        var server = ((CraftServer) Bukkit.getServer()).getServer();
        var executor = context.getSource().getExecutor();
        var level = ((CraftWorld) executor.getWorld()).getHandle();

        final var unpluggedPlayer = UnpluggedPlayerManager.createDummy(server, level, UUID.fromString(DUMMY_UUID), DUMMY_NAME);
        unpluggedPlayer.snapTo(executor.getX(), executor.getY(), executor.getZ(), executor.getYaw(), executor.getPitch());

        return 1;
    }
}
