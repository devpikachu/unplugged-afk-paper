package dev.detpikachu.unpluggedafk.player;

import com.mojang.authlib.GameProfile;
import dev.detpikachu.unpluggedafk.KickReasons;
import dev.detpikachu.unpluggedafk.api.UnpluggedPlayerInfo;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class UnpluggedServerPlayer extends ServerPlayer {

    // Copied verbatim from source mod:
    // Delay sending the ADD_PLAYER packets... because Mojang.
    private static final Duration SPAWN_PACKET_DELAY = Duration.ofMillis(200);
    private static final int PERIODIC_TICK_INTERVAL = 10;
    private static final int DEATH_LINGER_TICKS = 20;

    private final Session session;

    private boolean isSpawnStatePending = true;
    private boolean isDisconnectScheduled = false;
    private @Nullable Reason removeReason = null;
    private @Nullable Component deathMessage = null;

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

    public Session getSession() {
        return this.session;
    }

    public @Nullable Reason getRemoveReason() {
        return this.removeReason;
    }

    public UnpluggedPlayerInfo toInfo() {
        return new UnpluggedPlayerInfo(
                this.getUUID(),
                this.getPlainTextName(),
                this.session.durationMins(),
                this.session.reason(),
                this.session.startedAt(),
                this.session.expiresAt(),
                this.session.isFake());
    }

    public String describe() {
        return this.getPlainTextName() + " (" + this.getUUID() + ")";
    }

    @Override
    public void tick() {
        final var server = this.level().getServer();

        if (server.getTickCount() % PERIODIC_TICK_INTERVAL == 0) {
            this.tickPeriodic(server);
        }

        super.tick();
        this.doTick();
    }

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack previous, ItemStack stack) {
        if (this.isUsingItem()) {
            return;
        }

        super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void die(DamageSource damageSource) {
        this.dismount();
        super.die(damageSource);

        if (!this.isDeadOrDying()) {
            logDebug("Bot {} survived a cancelled death, so the session continues.", this.describe());
            return;
        }

        this.deathMessage = PaperAdventure.asAdventure(this.getCombatTracker().getDeathMessage());

        LOGGER.warn(
                "Bot {} died. Their items dropped and their spot is held for another {} tick(s).",
                this.describe(),
                DEATH_LINGER_TICKS);
    }

    @Override
    protected void tickDeath() {
        if (this.deathMessage == null) {
            super.tickDeath();
            return;
        }

        this.deathTime++;
        if (this.deathTime >= DEATH_LINGER_TICKS) {
            this.deferredDisconnect(this.deathMessage, Reason.DIED);
        }
    }

    @Override
    public ServerPlayer teleport(TeleportTransition transition) {
        super.teleport(transition);

        if (this.wonGame) {
            final var packet =
                    new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
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

        logDebug("Bot {} was removed from the world: {}.", this.describe(), reason);
        this.deferredDisconnect(Component.text(KickReasons.REMOVED), Reason.ENTITY_REMOVED);
    }

    public void deferredDisconnect(Component message, @Nullable Reason reason) {
        if (this.isDisconnectScheduled || this.connection.processedDisconnect) {
            return;
        }

        this.isDisconnectScheduled = true;
        this.removeReason = reason;

        LOGGER.info(
                "Killing bot {} after {} of {} minute(s): {}",
                this.describe(),
                this.session.elapsed().toMinutes(),
                this.session.durationMins(),
                PlainTextComponentSerializer.plainText().serialize(message));

        final var server = this.level().getServer();
        final var details = new DisconnectionDetails(PaperAdventure.asVanilla(message));
        server.schedule(new TickTask(server.getTickCount(), () -> this.connection.onDisconnect(details)));
    }

    private void tickPeriodic(MinecraftServer server) {
        if (this.isSpawnStatePending && this.session.elapsed().compareTo(SPAWN_PACKET_DELAY) >= 0) {
            this.applyDeferredSpawnState(server);
        }

        if (this.deathMessage != null && !this.isDeadOrDying()) {
            logDebug("Bot {} was healed mid-death, so the linger is dropped.", this.describe());
            this.deathMessage = null;
            this.deathTime = 0;
        }

        if (this.deathMessage == null && this.session.isExpired()) {
            this.deferredDisconnect(Component.text(KickReasons.EXPIRED), Reason.EXPIRED);
        }

        this.connection.resetPosition();
        this.level().getChunkSource().move(this);
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

        logDebug(
                "Sent deferred spawn packets for {} after {}ms.",
                this.describe(),
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

        for (final var entity : vehicle.getPassengers()) {
            if (entity instanceof Player) {
                entity.stopRiding();
            }
        }
    }
}
