package dev.detpikachu.unpluggedafk.compat.luckperms;

import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerSpawnEvent;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class LuckPermsListener implements Listener {

    private final LuckPermsBridge bridge;

    LuckPermsListener(LuckPermsBridge bridge) {
        this.bridge = bridge;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUnpluggedPlayerSpawn(UnpluggedPlayerSpawnEvent event) {
        if (event.getInfo().isFake()) {
            return;
        }

        final var bot = UnpluggedServerPlayer.from(event.getPlayer());

        if (bot != null) {
            this.bridge.attach(bot);
        }
    }
}
