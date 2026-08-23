package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class TabListener {

    private final TabBridge bridge;

    public TabListener(TabBridge bridge) {
        this.bridge = bridge;
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        this.bridge.refresh();
    }
}
