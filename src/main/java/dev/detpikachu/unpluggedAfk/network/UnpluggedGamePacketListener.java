package dev.detpikachu.unpluggedAfk.network;

import dev.detpikachu.unpluggedAfk.player.UnpluggedServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.NonNull;

public final class UnpluggedGamePacketListener extends ServerGamePacketListenerImpl {

    public UnpluggedGamePacketListener(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    @Override
    public void disconnect(@NonNull Component message)
    {
        UnpluggedServerPlayer sp = (UnpluggedServerPlayer) this.player;
        if (!sp.isValid()) { return; }

        if (message.getContents() instanceof TranslatableContents text &&
                (text.getKey().equals("multiplayer.disconnect.idling") ||
                        text.getKey().equals("multiplayer.disconnect.duplicate_login")))
        {
            sp.kill(message);
        }

        if (!ConfigWrap.unplugged().resetHealthUponDeath)
        {
            sp.kill(message);
        }
    }
}
