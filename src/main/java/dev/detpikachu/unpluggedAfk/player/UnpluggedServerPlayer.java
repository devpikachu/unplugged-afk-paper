package dev.detpikachu.unpluggedAfk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedAfk.UnpluggedConstants;
import dev.detpikachu.unpluggedAfk.config.UnpluggedOptions;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;

public final class UnpluggedServerPlayer extends ServerPlayer {

    private int durationMins = UnpluggedOptions.getInstance().getDefaultDurationMins();
    private long startAtMillis = System.currentTimeMillis();
    private long timeoutAtMillis = -1L;
    private long lastTickAtMillis = System.currentTimeMillis();
    private String reason = "";
    private @Nullable String outcome = null;

    private boolean isSpawnStatePending = true;
    private boolean isAlive = true;
    private boolean isTimedOut = false;

    public UnpluggedServerPlayer(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation) {
        super(server, level, gameProfile, clientInformation);
    }

    public int getDurationMins() {
        return this.durationMins;
    }

    public void setDurationMins(int durationMins) {
        // TODO: Not ideal since it swallows invalid input. Good enough for MVP.
        final var maxDurationMins = UnpluggedOptions.getInstance().getMaxDurationMins();

        if (durationMins > maxDurationMins) {
            durationMins = maxDurationMins;
        }

        this.durationMins = durationMins;
        this.timeoutAtMillis = System.currentTimeMillis() + (this.durationMins * 60000L);
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void kill(Component message) {
        this.killBot();

        if (message.getContents() instanceof TranslatableContents text && text.getKey().equals(UnpluggedConstants.DISCONNECT_DUPLICATE_LOGIN)) {
            this.connection.onDisconnect(new DisconnectionDetails(message));
        }

        final var server = this.level().getServer();
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

    private void applyDeferredSpawnState(MinecraftServer server) {
        final var playerList = server.getPlayerList();

        playerList.broadcastAll(new ClientboundRotateHeadPacket(this, (byte) (this.yHeadRot * 256 / 360)), this.level().dimension());
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, this));
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, this));

        this.isSpawnStatePending = false;
    }

    private void killBot() {
        // TODO
    }

    private void tickBot(MinecraftServer server) {
        final long now = System.currentTimeMillis();

        final long delta = now - this.lastTickAtMillis;
        this.lastTickAtMillis = now;

        // TODO: Entry ops
    }

    private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, final UUID uuid) {
        final ResolvableProfile resolver = ResolvableProfile.createUnresolved(uuid);
        return resolver.resolveProfile(server.services().profileResolver());
    }
}
