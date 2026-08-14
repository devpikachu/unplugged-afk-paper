package dev.detpikachu.unpluggedAfk.player;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;

public final class UnpluggedServerPlayer extends ServerPlayer {

    public UnpluggedServerPlayer(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation) {
        super(server, level, gameProfile, clientInformation);
    }

    private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, final UUID uuid) {
        final ResolvableProfile resolver = ResolvableProfile.createUnresolved(uuid);
        return resolver.resolveProfile(server.services().profileResolver());
    }

    public static void createFromPlayer(MinecraftServer server, ServerPlayer player, int time, String reason) {
        if (time < 1)
        {
            // TODO: Fetch from config
            time = 480; // 480 m = 8 h
        }

        // TODO: Rest of method
    }
}
