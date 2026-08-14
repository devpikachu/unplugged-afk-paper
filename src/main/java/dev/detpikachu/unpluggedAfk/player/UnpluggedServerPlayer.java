package dev.detpikachu.unpluggedAfk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.UnpluggedConstants;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;

public final class UnpluggedServerPlayer extends ServerPlayer {

    private boolean isSpawnStatePending = true;
    private int durationMinutes = 0;
    private String reason;
    private String outcome;
    private long startAtMillis;
    private long timeoutMillis = -1L;
    private long lastTickMillis;
    private boolean isAlive = true;
    private boolean isTimedOut = false;

    public UnpluggedServerPlayer(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation) {
        super(server, level, gameProfile, clientInformation);
        startAtMillis = System.currentTimeMillis();
        lastTickMillis = System.currentTimeMillis();
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

    @Override
    public void tick() {
        final var server = this.level().getServer();

        if (server.getTickCount() % 10 != 0) {
            try {
                super.tick();
                this.doTick();
            } catch (NullPointerException exception) {
                // Do nothing
            }

            return;
        }

        if (this.isSpawnStatePending) {
            final var now = System.currentTimeMillis();

            // Copied verbatim from source mod:
            // Delay sending the ADD_PLAYER packets... because Mojang.
            if ((now - this.startAtMillis) >= 200L) {
                this.applyDeferredSpawnState(server);
            }
        }

        // Remove invalid bots that are still ticking
        if (!this.isSpawnStatePending && !this.isAlive) {
            final var name = this.getName();
            final var outcome = Component.literal("Invalid");

            this.kill(outcome);
            server.getPlayerList().remove(this);

            // TODO: Send leave message?
        }

        this.tickBot(server);
        this.connection.resetPosition();
        this.level().getChunkSource().move(this);

        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException exception) {
            // Do nothing
        }
    }

    private void tickBot(MinecraftServer server) {
        final long now = System.currentTimeMillis();

        final long delta = now - this.lastTickMillis;
        this.lastTickMillis = now;

        // TODO: Entry ops
    }

    private void applyDeferredSpawnState(MinecraftServer server) {
        var playerList = server.getPlayerList();

        playerList.broadcastAll(new ClientboundRotateHeadPacket(this, (byte) (this.yHeadRot * 256 / 360)), this.level().dimension());
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, this));
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, this));

        this.isSpawnStatePending = false;
    }

    private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, final UUID uuid) {
        final ResolvableProfile resolver = ResolvableProfile.createUnresolved(uuid);
        return resolver.resolveProfile(server.services().profileResolver());
    }
}
