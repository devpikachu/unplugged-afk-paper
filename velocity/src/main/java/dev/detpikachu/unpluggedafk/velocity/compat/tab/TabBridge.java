package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import com.velocitypowered.api.proxy.Player;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

@ApiStatus.Internal
public final class TabBridge {

    private static final String LOCATOR_TAB = "me.neznamy.tab.shared.TAB";
    private static final String LOCATOR_CPU_MANAGER = "me.neznamy.tab.shared.cpu.CpuManager";
    private static final String LOCATOR_FEATURE_MANAGER = "me.neznamy.tab.shared.FeatureManager";
    private static final String LOCATOR_TAB_PLAYER = "me.neznamy.tab.shared.platform.TabPlayer";
    private static final String LOCATOR_VELOCITY_PLATFORM = "me.neznamy.tab.platforms.velocity.VelocityPlatform";
    private static final String LOCATOR_VELOCITY_TAB_PLAYER = "me.neznamy.tab.platforms.velocity.VelocityTabPlayer";

    private final Method getInstance;
    private final Method isPluginDisabled;
    private final Method getPlayer;
    private final Method getPlatform;
    private final Method getCpuManager;
    private final Method runTask;
    private final Method getFeatureManager;
    private final Method onJoin;
    private final Method onQuit;
    private final Class<?> velocityTabPlayerClass;
    private final Constructor<?> velocityTabPlayer;
    private final Method playerOf;

    private TabBridge() throws ReflectiveOperationException {
        final var tabClass = Class.forName(LOCATOR_TAB);
        final var cpuManagerClass = Class.forName(LOCATOR_CPU_MANAGER);
        final var featureManagerClass = Class.forName(LOCATOR_FEATURE_MANAGER);
        final var tabPlayerClass = Class.forName(LOCATOR_TAB_PLAYER);
        final var velocityPlatformClass = Class.forName(LOCATOR_VELOCITY_PLATFORM);

        this.velocityTabPlayerClass = Class.forName(LOCATOR_VELOCITY_TAB_PLAYER);

        this.getInstance = tabClass.getMethod("getInstance");
        this.isPluginDisabled = tabClass.getMethod("isPluginDisabled");
        this.getPlayer = tabClass.getMethod("getPlayer", UUID.class);
        this.getPlatform = tabClass.getMethod("getPlatform");
        this.getCpuManager = tabClass.getMethod("getCPUManager");
        this.runTask = cpuManagerClass.getMethod("runTask", Runnable.class);
        this.getFeatureManager = tabClass.getMethod("getFeatureManager");
        this.onJoin = featureManagerClass.getMethod("onJoin", tabPlayerClass);
        this.onQuit = featureManagerClass.getMethod("onQuit", tabPlayerClass);
        this.velocityTabPlayer = this.velocityTabPlayerClass.getConstructor(velocityPlatformClass, Player.class);
        this.playerOf = this.velocityTabPlayerClass.getMethod("getPlayer");
    }

    static @Nullable TabBridge resolve() {
        try {
            return new TabBridge();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Log.warn("Could not resolve TAB's internals.", exception);
            return null;
        }
    }

    public void addBot(Player bot) {
        Log.debug("Handing bot {} ({}) to TAB.", bot.getUsername(), bot.getUniqueId());
        this.dispatch(tab -> this.join(tab, bot));
    }

    public void removeBot(Player bot) {
        this.dispatch(tab -> this.quit(tab, bot));
    }

    private void join(Object tab, Player bot) throws ReflectiveOperationException {
        final var existing = this.getPlayer.invoke(tab, bot.getUniqueId());

        if (existing != null) {
            if (!this.holds(existing, bot)) {
                Log.warn(
                        "TAB still tracks {} ({}) as a real player, so their bot was not handed to it.",
                        bot.getUsername(),
                        bot.getUniqueId());
            }

            return;
        }

        final var player = this.velocityTabPlayer.newInstance(this.getPlatform.invoke(tab), bot);
        this.onJoin.invoke(this.getFeatureManager.invoke(tab), player);
    }

    private void quit(Object tab, Player bot) throws ReflectiveOperationException {
        final var existing = this.getPlayer.invoke(tab, bot.getUniqueId());

        if (existing != null && this.holds(existing, bot)) {
            this.onQuit.invoke(this.getFeatureManager.invoke(tab), existing);
        }
    }

    private boolean holds(Object tabPlayer, Player bot) throws ReflectiveOperationException {
        return this.velocityTabPlayerClass.isInstance(tabPlayer) && bot.equals(this.playerOf.invoke(tabPlayer));
    }

    private void dispatch(Call call) {
        try {
            final var tab = this.getInstance.invoke(null);

            if (tab == null || (boolean) this.isPluginDisabled.invoke(tab)) {
                return;
            }

            this.runTask.invoke(this.getCpuManager.invoke(tab), (Runnable) () -> this.run(tab, call));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Log.warn("Could not reach TAB's processing thread.", exception);
        }
    }

    private void run(Object tab, Call call) {
        try {
            call.invoke(tab);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Log.warn("TAB rejected a bot.", exception);
        }
    }

    @FunctionalInterface
    private interface Call {

        void invoke(Object tab) throws ReflectiveOperationException;
    }
}
