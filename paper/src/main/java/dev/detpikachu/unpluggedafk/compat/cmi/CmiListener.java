package dev.detpikachu.unpluggedafk.compat.cmi;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.ModuleHandling.CMIModule;
import com.Zrips.CMI.events.CMIAfkEnterEvent.AfkType;
import com.Zrips.CMI.events.CMIAfkKickEvent;
import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerRemoveEvent;
import dev.detpikachu.unpluggedafk.api.events.UnpluggedPlayerSpawnEvent;
import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApiStatus.Internal
public final class CmiListener implements Listener {

    private static final String PERMISSION_KICK_BYPASS = "cmi.command.kick.bypass";

    private final UnpluggedAfk plugin;
    private final Map<UUID, PermissionAttachment> kickBypassRevocations = new HashMap<>();

    CmiListener(UnpluggedAfk plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUnpluggedPlayerSpawn(UnpluggedPlayerSpawnEvent event) {
        final var bot = event.getPlayer();

        this.kickBypassRevocations.put(
                bot.getUniqueId(), bot.addAttachment(this.plugin, PERMISSION_KICK_BYPASS, false));

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
        final var uuid = event.getPlayer().getUniqueId();
        final var revocation = this.kickBypassRevocations.remove(uuid);

        if (revocation != null) {
            revocation.remove();
        }

        CMI.getInstance().getAfkManager().removeAfkInfo(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAfkKick(CMIAfkKickEvent event) {
        final var player = event.getPlayer();

        if (player != null && UnpluggedServerPlayer.from(player) != null) {
            event.setCancelled(true);
        }
    }
}
