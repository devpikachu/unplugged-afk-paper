package dev.detpikachu.unpluggedAfk.commands.admin.debug;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.detpikachu.unpluggedAfk.network.UnpluggedConnection;
import dev.detpikachu.unpluggedAfk.network.UnpluggedGamePacketListener;
import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.util.KeepAlive;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

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
        var playerList = server.getPlayerList();

        var profile = new GameProfile(UUID.fromString(DUMMY_UUID), DUMMY_NAME);
        var clientInformation = ClientInformation.createDefault();
        var cookie = new CommonListenerCookie(profile, 0, clientInformation, true, null, new HashSet<>(), new KeepAlive());
        var connection = new UnpluggedConnection(PacketFlow.SERVERBOUND);
        var player = new UnpluggedServerPlayer(server, level, profile, clientInformation);

        playerList.placeNewPlayer(connection, player, cookie);
        player.connection = new UnpluggedGamePacketListener(server, connection, player, cookie);
        player.snapTo(executor.getX(), executor.getY(), executor.getZ(), executor.getPitch(), executor.getYaw());
        player.gameMode.changeGameModeForPlayer(GameType.DEFAULT_MODE, PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null);

        return 1;
    }
}
