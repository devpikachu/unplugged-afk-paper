package dev.detpikachu.unpluggedafk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.network.UnpluggedConnection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.NonNull;

import static dev.detpikachu.unpluggedafk.UnpluggedConstants.KILL_REASON_EXPIRED;
import static dev.detpikachu.unpluggedafk.UnpluggedConstants.KILL_REASON_REMOVED;

public final class UnpluggedServerPlayer extends ServerPlayer {

    private static final long MILLIS_PER_MINUTE = 60_000L;

    private final UnpluggedSession session;
    private final long startAtMillis = System.currentTimeMillis();
    private final long timeoutAtMillis;

    private boolean isSpawnStatePending = true;
    private boolean isDisconnectScheduled = false;

    public UnpluggedServerPlayer(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, UnpluggedSession session) {
        super(server, level, gameProfile, clientInformation);
        this.session = session;
        this.timeoutAtMillis = this.startAtMillis + (session.durationMins() * MILLIS_PER_MINUTE);
    }

    public boolean isFake() {
        return this.session.isFake();
    }

    public int getDurationMins() {
        return this.session.durationMins();
    }

    public String getReason() {
        return this.session.reason();
    }

    public long getStartAtMillis() {
        return this.startAtMillis;
    }

    public long getTimeoutAtMillis() {
        return this.timeoutAtMillis;
    }

    @Override
    public void tick() {
        final var server = this.level().getServer();

        if (server.getTickCount() % 10 != 0) {
            super.tick();
            this.doTick();
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

        if (System.currentTimeMillis() >= this.timeoutAtMillis) {
            this.deferredDisconnect(Component.literal(KILL_REASON_EXPIRED));
        }

        this.connection.resetPosition();
        this.level().getChunkSource().move(this);

        super.tick();
        this.doTick();
    }

    @Override
    public void onEquipItem(final @NonNull EquipmentSlot slot, final @NonNull ItemStack previous, final @NonNull ItemStack stack) {
        if (this.isUsingItem()) {
            return;
        }

        super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void die(@NonNull DamageSource damageSource) {
        this.dismount();
        super.die(damageSource);

        UnpluggedAfk.LOGGER.warn("Unplugged player {} ({}) died. Their items dropped and their spot is no longer held.", this.getName().getString(), this.getUUID());
        this.deferredDisconnect(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public ServerPlayer teleport(@NonNull TeleportTransition transition) {
        super.teleport(transition);

        if (this.wonGame) {
            var packet = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
            this.connection.handleClientCommand(packet);
        }

        if (this.connection.player.isChangingDimension()) {
            this.connection.player.hasChangedDimension();
        }

        return this.connection.player;
    }

    @Override
    public void onRemoval(@NonNull RemovalReason reason) {
        super.onRemoval(reason);

        if (reason == RemovalReason.CHANGED_DIMENSION) {
            return;
        }

        UnpluggedAfk.logDebug("Unplugged player {} ({}) was removed from the world: {}.", this.getName().getString(), this.getUUID(), reason);
        this.deferredDisconnect(Component.literal(KILL_REASON_REMOVED));
    }

    public void deferredDisconnect(Component message) {
        if (this.isDisconnectScheduled || this.connection.processedDisconnect) {
            return;
        }
        this.isDisconnectScheduled = true;

        UnpluggedAfk.LOGGER.info(
                "Killing unplugged player {} ({}) after {} of {} minute(s): {}",
                this.getName().getString(),
                this.getUUID(),
                (System.currentTimeMillis() - this.startAtMillis) / MILLIS_PER_MINUTE,
                this.session.durationMins(),
                message.getString()
        );

        final var server = this.level().getServer();
        server.schedule(new TickTask(server.getTickCount(), () -> this.connection.onDisconnect(new DisconnectionDetails(message))));
    }

    public void loadPersistedData() {
        try (var reporter = new ProblemReporter.ScopedCollector(this.problemPath(), UnpluggedAfk.LOGGER)) {
            final var persisted = this
                    .level()
                    .getServer()
                    .getPlayerList()
                    .loadPlayerData(this.nameAndId());

            if (persisted.isEmpty()) {
                UnpluggedAfk.LOGGER.warn("No persisted data for {} ({}). The bot holds world spawn with an empty inventory.", this.getName().getString(), this.getUUID());
                return;
            }

            final var data = TagValueInput.create(reporter, this.registryAccess(), persisted.get());

            this.load(data);
            this.loadAndSpawnEnderPearls(data);
            this.loadAndSpawnParentVehicle(data);
        }
    }

    private void applyDeferredSpawnState(MinecraftServer server) {
        final var playerList = server.getPlayerList();

        playerList.broadcastAll(new ClientboundRotateHeadPacket(this, (byte) (this.yHeadRot * 256 / 360)), this.level().dimension());
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, this));
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, this));

        this.isSpawnStatePending = false;

        UnpluggedAfk.logDebug("Sent deferred spawn packets for {} ({}) after {}ms.", this.getName().getString(), this.getUUID(), System.currentTimeMillis() - this.startAtMillis);
    }

    private void dismount() {
        final var vehicle = this.getVehicle();

        if (vehicle == null) {
            return;
        }

        if (vehicle instanceof Player) {
            this.stopRiding();
        }

        for (var entity : vehicle.getPassengers()) {
            if (entity instanceof Player) {
                entity.stopRiding();
            }
        }
    }
}
