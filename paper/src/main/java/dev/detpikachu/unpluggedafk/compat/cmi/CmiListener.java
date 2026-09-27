package dev.detpikachu.unpluggedafk.compat.cmi;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.ModuleHandling.CMIModule;
import com.Zrips.CMI.events.CMIAfkEnterEvent.AfkType;
import com.Zrips.CMI.events.CMIAfkKickEvent;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerSpawnEvent;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class CmiListener implements Listener {

    @EventHandler
    public void onUnpluggedPlayerSpawn(UnpluggedPlayerSpawnEvent event) {
        final var bot = event.getPlayer();
        final var cmi = CMI.getInstance();

        if (!CMIModule.afk.isEnabled() || cmi.getAfkManager().isDisabledWorld(bot.getWorld())) {
            return;
        }

        final var user = cmi.getPlayerManager().getUser(bot);

        if (user != null) {
            user.setAfk(true, AfkType.manual);
        }
    }

    @EventHandler
    public void onUnpluggedPlayerRemove(UnpluggedPlayerRemoveEvent event) {
        CMI.getInstance().getAfkManager().removeAfkInfo(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAfkKick(CMIAfkKickEvent event) {
        final var player = event.getPlayer();

        if (player != null && UnpluggedServerPlayer.from(player) != null) {
            event.setCancelled(true);
        }
    }
}
