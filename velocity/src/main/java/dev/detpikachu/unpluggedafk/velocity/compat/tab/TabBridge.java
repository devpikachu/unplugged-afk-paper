package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.session.Session;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity.logDebug;

@ApiStatus.Internal
public final class TabBridge {

    private static final String FEATURE_NAME = "GlobalPlayerList";

    private static final int RESYNC_DELAY_SECS = 2;

    private static final String LOCATOR_TAB = "me.neznamy.tab.shared.TAB";
    private static final String LOCATOR_FEATURE_MANAGER = "me.neznamy.tab.shared.FeatureManager";
    private static final String LOCATOR_GLOBAL_PLAYER_LIST =
            "me.neznamy.tab.shared.features.globalplayerlist.GlobalPlayerList";
    private static final String LOCATOR_PROXY_PLAYER = "me.neznamy.tab.shared.features.proxy.ProxyPlayer";
    private static final String LOCATOR_SERVER = "me.neznamy.tab.shared.data.Server";
    private static final String LOCATOR_SKIN = "me.neznamy.tab.shared.platform.TabList$Skin";
    private static final String LOCATOR_THREAD_EXECUTOR = "me.neznamy.tab.shared.cpu.ThreadExecutor";

    private final UnpluggedAfkVelocity plugin;
    private final ProxyServer proxyServer;
    private final Logger logger;

    private final Method getInstance;
    private final Method getFeatureManager;
    private final Method getFeature;
    private final Method getCustomThread;
    private final Method execute;
    private final Method onJoin;
    private final Method serverByName;
    private final Constructor<?> skin;
    private final Constructor<?> proxyPlayer;
    private final Method onQuit;

    private final ConcurrentHashMap<UUID, Object> bots = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshPending = new AtomicBoolean();

    private TabBridge(UnpluggedAfkVelocity plugin) throws ReflectiveOperationException {
        final var tabClass = Class.forName(LOCATOR_TAB);
        final var featureManagerClass = Class.forName(LOCATOR_FEATURE_MANAGER);
        final var globalPlayerListClass = Class.forName(LOCATOR_GLOBAL_PLAYER_LIST);
        final var proxyPlayerClass = Class.forName(LOCATOR_PROXY_PLAYER);
        final var serverClass = Class.forName(LOCATOR_SERVER);
        final var skinClass = Class.forName(LOCATOR_SKIN);
        final var threadExecutorClass = Class.forName(LOCATOR_THREAD_EXECUTOR);

        this.plugin = plugin;
        this.proxyServer = plugin.getProxyServer();
        this.logger = plugin.getLogger();

        this.getInstance = tabClass.getMethod("getInstance");
        this.getFeatureManager = tabClass.getMethod("getFeatureManager");
        this.getFeature = featureManagerClass.getMethod("getFeature", String.class);
        this.getCustomThread = globalPlayerListClass.getMethod("getCustomThread");
        this.execute = threadExecutorClass.getMethod("execute", Runnable.class);
        this.onJoin = globalPlayerListClass.getMethod("onJoin", proxyPlayerClass);
        this.serverByName = serverClass.getMethod("byName", String.class);
        this.skin = skinClass.getConstructor(String.class, String.class);
        this.proxyPlayer = proxyPlayerClass.getConstructor(
                UUID.class, UUID.class, String.class, serverClass, boolean.class, boolean.class, skinClass);
        this.onQuit = globalPlayerListClass.getMethod("onQuit", proxyPlayerClass);
    }

    static @Nullable TabBridge resolve(UnpluggedAfkVelocity plugin) {
        try {
            return new TabBridge(plugin);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warn("Could not resolve TAB's internals.", exception);
            return null;
        }
    }

    public Set<UUID> trackedBots() {
        return Set.copyOf(this.bots.keySet());
    }

    public void addBot(String serverName, UUID uuid, String username, Session.@Nullable Skin skin) {
        logDebug("Building a TAB entry for bot {} ({}) on {}.", username, uuid, serverName);
        this.dispatch(feature -> this.bots.put(uuid, this.newProxyPlayer(uuid, username, serverName, skin)));
        this.refreshLater();
    }

    public void refreshLater() {
        if (!this.refreshPending.compareAndSet(false, true)) {
            return;
        }

        this.proxyServer
                .getScheduler()
                .buildTask(this.plugin, () -> {
                    this.refreshPending.set(false);
                    this.refresh();
                })
                .delay(Duration.ofSeconds(RESYNC_DELAY_SECS))
                .schedule();
    }

    public void removeBot(UUID uuid) {
        this.dispatch(feature -> {
            final var bot = this.bots.remove(uuid);

            if (bot != null) {
                this.onQuit.invoke(feature, bot);
            }
        });
    }

    public void refresh() {
        if (this.bots.isEmpty()) {
            return;
        }

        logDebug("Re-asserting {} TAB entr(ies) to every viewer.", this.bots.size());
        this.dispatch(feature -> {
            for (final var bot : this.bots.values()) {
                this.onJoin.invoke(feature, bot);
            }
        });
    }

    private void dispatch(Call call) {
        final var feature = this.globalPlayerList();

        if (feature == null) {
            logDebug("TAB has no GlobalPlayerList feature, so bots stay backend-local.");
            return;
        }

        try {
            this.execute.invoke(this.getCustomThread.invoke(feature), (Runnable) () -> this.run(feature, call));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            this.logger.warn("Could not reach TAB's feature thread.", exception);
        }
    }

    private void run(Object feature, Call call) {
        try {
            call.invoke(feature);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            this.logger.warn("TAB rejected a bot's tab list entry.", exception);
        }
    }

    private @Nullable Object globalPlayerList() {
        try {
            final var tab = this.getInstance.invoke(null);

            if (tab == null) {
                return null;
            }

            return this.getFeature.invoke(this.getFeatureManager.invoke(tab), FEATURE_NAME);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            this.logger.warn("Could not ask TAB for its global player list.", exception);
            return null;
        }
    }

    private Object newProxyPlayer(UUID uuid, String username, String serverName, Session.@Nullable Skin skin)
            throws ReflectiveOperationException {
        final var server = this.serverByName.invoke(null, serverName);
        final var texture = skin == null ? null : this.skin.newInstance(skin.value(), skin.signature());

        return this.proxyPlayer.newInstance(uuid, uuid, username, server, false, false, texture);
    }

    @FunctionalInterface
    private interface Call {

        void invoke(Object feature) throws ReflectiveOperationException;
    }
}
