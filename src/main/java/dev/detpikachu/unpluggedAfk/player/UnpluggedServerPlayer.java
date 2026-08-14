package dev.detpikachu.unpluggedAfk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.UnpluggedConstants;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;

public final class UnpluggedServerPlayer extends ServerPlayer {

    private boolean isSpawnStatePending;
    private long placedAtMillis;
    private int durationMinutes;
    private String reason;
    private String outcome;
    private long startAtMillis;
    private long timeoutMillis;
    private long lastTickMillis;
    private boolean isAlive;
    private boolean isTimedOut;

    public UnpluggedServerPlayer(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation) {
        super(server, level, gameProfile, clientInformation);
    }

    private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, final UUID uuid) {
        final ResolvableProfile resolver = ResolvableProfile.createUnresolved(uuid);
        return resolver.resolveProfile(server.services().profileResolver());
    }

    public int getDurationMinutes() {
        return this.durationMinutes;
    }

    public String getReason() {
        return this.reason;
    }

    public long getStartAtMillis() {
        return this.startAtMillis;
    }

    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public void kill(Component message) {
        // TODO: Kill shadow

        if (message.getContents() instanceof TranslatableContents text && text.getKey().equals(UnpluggedConstants.DISCONNECT_DUPLICATE_LOGIN)) {
            this.connection.onDisconnect(new DisconnectionDetails(message));
        }

        var server = this.level().getServer();
        server.schedule(
                new TickTask(server.getTickCount(), () -> {
                    this.connection.onDisconnect(new DisconnectionDetails(message));
                })
        );
    }
}
