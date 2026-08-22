package dev.detpikachu.unpluggedafk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedafk.Constants.KickReasons;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent.Reason;
import dev.detpikachu.unpluggedafk.session.Session;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.DisconnectionDetails;
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
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

@ApiStatus.Internal
public final class UnpluggedServerPlayer extends ServerPlayer {

    // Copied verbatim from source mod:
    // Delay sending the ADD_PLAYER packets... because Mojang.
    private static final Duration SPAWN_PACKET_DELAY = Duration.ofMillis(200);

    private final Session session;

    private boolean isSpawnStatePending = true;
    private boolean isDisconnectScheduled = false;
    private @Nullable Reason removeReason = null;

    public UnpluggedServerPlayer(
            MinecraftServer server,
            ServerLevel level,
            GameProfile gameProfile,
            ClientInformation clientInformation,
            Session session) {
        super(server, level, gameProfile, clientInformation);
        this.session = session;
    }

    public static @Nullable UnpluggedServerPlayer from(org.bukkit.entity.Player player) {
        return ((CraftPlayer) player).getHandle() instanceof UnpluggedServerPlayer bot ? bot : null;
    }

    public int getDurationMins() {
        return this.session.durationMins();
    }

    public String getReason() {
        return this.session.reason();
    }

    public Instant getStartedAt() {
        return this.session.startedAt();
    }

    public Instant getExpiresAt() {
        return this.session.expiresAt();
    }

    public Duration elapsed() {
        return this.session.elapsed();
    }

    public Duration remaining() {
        return this.session.remaining();
    }

    public boolean isFake() {
        return this.session.isFake();
    }

    public @Nullable Reason getRemoveReason() {
        return this.removeReason;
    }

    public void setRemoveReason(@Nullable Reason reason) {
        this.removeReason = reason;
    }

    @Override
    public void tick() {
        final var server = this.level().getServer();

        if (server.getTickCount() % 10 != 0) {
            super.tick();
            this.doTick();
            return;
        }

        if (this.isSpawnStatePending && this.session.elapsed().compareTo(SPAWN_PACKET_DELAY) >= 0) {
            this.applyDeferredSpawnState(server);
        }

        if (this.session.isExpired()) {
            this.deferredDisconnect(Component.text(KickReasons.EXPIRED), Reason.EXPIRED);
        }

        this.connection.resetPosition();
        this.level().getChunkSource().move(this);

        super.tick();
        this.doTick();
    }

    @Override
    public void onEquipItem(final EquipmentSlot slot, final ItemStack previous, final ItemStack stack) {
        if (this.isUsingItem()) {
            return;
        }

        super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void die(DamageSource damageSource) {
        this.dismount();
        super.die(damageSource);

        LOGGER.warn(
                "Bot {} ({}) died. Their items dropped and their spot is no longer held.",
                this.getName().getString(),
                this.getUUID());
        this.deferredDisconnect(PaperAdventure.asAdventure(this.getCombatTracker().getDeathMessage()), Reason.DIED);
    }

    @Override
    public ServerPlayer teleport(TeleportTransition transition) {
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
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);

        if (reason == RemovalReason.CHANGED_DIMENSION) {
            return;
        }

        UnpluggedAfk.logDebug(
                "Bot {} ({}) was removed from the world: {}.",
                this.getName().getString(),
                this.getUUID(),
                reason);
        this.deferredDisconnect(Component.text(KickReasons.REMOVED), Reason.ENTITY_REMOVED);
    }

    public void deferredDisconnect(Component message, @Nullable Reason reason) {
        if (this.isDisconnectScheduled || this.connection.processedDisconnect) {
            return;
        }

        this.isDisconnectScheduled = true;
        this.setRemoveReason(reason);

        LOGGER.info(
                "Killing bot {} ({}) after {} of {} minute(s): {}",
                this.getName().getString(),
                this.getUUID(),
                this.session.elapsed().toMinutes(),
                this.session.durationMins(),
                PlainTextComponentSerializer.plainText().serialize(message));

        final var server = this.level().getServer();
        final var details = new DisconnectionDetails(PaperAdventure.asVanilla(message));
        server.schedule(new TickTask(server.getTickCount(), () -> this.connection.onDisconnect(details)));
    }

    public void loadPersistedData() {
        try (var reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            final var persisted = this.level().getServer().getPlayerList().loadPlayerData(this.nameAndId());

            if (persisted.isEmpty()) {
                LOGGER.warn(
                        "No persisted data for {} ({}). The bot holds world spawn with an empty inventory.",
                        this.getName().getString(),
                        this.getUUID());
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

        playerList.broadcastAll(
                new ClientboundRotateHeadPacket(this, (byte) (this.yHeadRot * 256 / 360)),
                this.level().dimension());
        playerList.broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, this));
        playerList.broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, this));

        this.isSpawnStatePending = false;

        UnpluggedAfk.logDebug(
                "Sent deferred spawn packets for {} ({}) after {}ms.",
                this.getName().getString(),
                this.getUUID(),
                this.session.elapsed().toMillis());
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
