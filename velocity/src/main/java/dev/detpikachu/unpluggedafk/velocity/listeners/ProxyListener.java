package dev.detpikachu.unpluggedafk.velocity.listeners;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.compat.tab.TabBridge;
import dev.detpikachu.unpluggedafk.velocity.network.BotPlayerBridge;
import dev.detpikachu.unpluggedafk.velocity.network.LinkServer;
import dev.detpikachu.unpluggedafk.velocity.session.SessionStore;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import static net.kyori.adventure.text.Component.text;

@ApiStatus.Internal
public final class ProxyListener {

    private static final Component UNPLUGGED = text("You have unplugged.");

    private final ProxyServer proxyServer;
    private final SessionStore sessionStore;
    private final LinkServer linkServer;
    private final BotPlayerBridge botPlayerBridge;
    private final @Nullable TabBridge tabBridge;

    public ProxyListener(UnpluggedAfkVelocity plugin, BotPlayerBridge botPlayerBridge, @Nullable TabBridge tabBridge) {
        this.proxyServer = plugin.getProxyServer();
        this.sessionStore = plugin.getSessionStore();
        this.linkServer = plugin.getLinkServer();
        this.botPlayerBridge = botPlayerBridge;
        this.tabBridge = tabBridge;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        Log.debug("PreLogin for {}, clearing any presence under that name.", event.getUsername());
        this.botPlayerBridge.remove(event.getUsername());
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        Log.debug(
                "GameProfileRequest for {} ({}), clearing any presence under that uuid.",
                event.getUsername(),
                event.getGameProfile().getId());
        this.botPlayerBridge.remove(event.getGameProfile().getId());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        final var player = event.getPlayer();
        Log.debug(
                "Disconnect of {} ({}), materialising any pending presence.",
                player.getUsername(),
                player.getUniqueId());
        this.botPlayerBridge.addPending(player.getUniqueId());
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        final var uuid = event.getPlayer().getUniqueId();
        final var serverName = event.getServer().getServerInfo().getName();

        if (!this.sessionStore.isHeldBy(uuid, serverName)) {
            Log.debug(
                    "{} ({}) was kicked from {} without holding a session there, so the kick is left alone.",
                    event.getPlayer().getUsername(),
                    uuid,
                    serverName);
            return;
        }

        Log.debug(
                "{} ({}) unplugged on {}, so the kick becomes a disconnect.",
                event.getPlayer().getUsername(),
                uuid,
                serverName);
        event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                event.getServerKickReason().orElse(UNPLUGGED)));
    }

    @Subscribe
    public EventTask onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        return EventTask.async(() -> {
            this.linkServer.awaitSettled();
            this.chooseInitialServer(event);
        });
    }

    private void chooseInitialServer(PlayerChooseInitialServerEvent event) {
        final var player = event.getPlayer();
        if (this.tabBridge != null) {
            this.tabBridge.removeBot(player.getUniqueId());
        }

        final var session = this.sessionStore.consume(player.getUniqueId());
        if (session.isEmpty()) {
            return;
        }

        final var serverName = session.get().serverName();
        final var server = this.proxyServer.getServer(serverName);

        if (server.isEmpty()) {
            Log.warn(
                    "{} ({}) unplugged on {}, which is no longer registered, so they fall back to the try list.",
                    player.getUsername(),
                    player.getUniqueId(),
                    serverName);
            return;
        }

        event.setInitialServer(server.get());
        Log.info("Routing {} ({}) back to {}", player.getUsername(), player.getUniqueId(), serverName);
    }
}
