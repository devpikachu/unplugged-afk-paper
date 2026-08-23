package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.velocity.Constants.Sessions;
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

@ApiStatus.Internal
public final class TabBridge {

    private static final String FEATURE_NAME = "GlobalPlayerList";

    private static final String TAB = "me.neznamy.tab.shared.TAB";
    private static final String FEATURE_MANAGER = "me.neznamy.tab.shared.FeatureManager";
    private static final String GLOBAL_PLAYER_LIST = "me.neznamy.tab.shared.features.globalplayerlist.GlobalPlayerList";
    private static final String PROXY_PLAYER = "me.neznamy.tab.shared.features.proxy.ProxyPlayer";
    private static final String SERVER = "me.neznamy.tab.shared.data.Server";
    private static final String SKIN = "me.neznamy.tab.shared.platform.TabList$Skin";
    private static final String THREAD_EXECUTOR = "me.neznamy.tab.shared.cpu.ThreadExecutor";

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

    private TabBridge(UnpluggedAfkVelocity plugin, ProxyServer proxyServer, Logger logger)
            throws ReflectiveOperationException {
        final var tabClass = Class.forName(TAB);
        final var featureManagerClass = Class.forName(FEATURE_MANAGER);
        final var globalPlayerListClass = Class.forName(GLOBAL_PLAYER_LIST);
        final var proxyPlayerClass = Class.forName(PROXY_PLAYER);
        final var serverClass = Class.forName(SERVER);
        final var skinClass = Class.forName(SKIN);
        final var threadExecutorClass = Class.forName(THREAD_EXECUTOR);

        this.plugin = plugin;
        this.proxyServer = proxyServer;
        this.logger = logger;

        this.getInstance = tabClass.getMethod("getInstance");
        this.getFeatureManager = tabClass.getMethod("getFeatureManager");
        this.getFeature = featureManagerClass.getMethod("getFeature", String.class);
        this.getCustomThread = globalPlayerListClass.getMethod("getCustomThread");
        this.execute = threadExecutorClass.getMethod("execute", Runnable.class);
        this.onJoin = globalPlayerListClass.getMethod("onJoin", proxyPlayerClass);
        this.serverByName = serverClass.getMethod("byName", String.class);
        this.skin = skinClass.getConstructor(String.class, String.class);
        this.proxyPlayer = proxyPlayerClass.getConstructor(
                UUID.class,
                UUID.class,
                String.class,
                serverClass,
                boolean.class,
                boolean.class,
                skinClass);
        this.onQuit = globalPlayerListClass.getMethod("onQuit", proxyPlayerClass);
    }

    static @Nullable TabBridge resolve(UnpluggedAfkVelocity plugin, ProxyServer proxyServer, Logger logger) {
        try {
            return new TabBridge(plugin, proxyServer, logger);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.warn("Could not resolve TAB's internals.", exception);
            return null;
        }
    }

    public Set<UUID> trackedBots() {
        return Set.copyOf(this.bots.keySet());
    }

    public void addBot(String serverName, UUID uuid, String username, Session.@Nullable Skin skin) {
        this.dispatch(feature -> {
            final var bot = this.newProxyPlayer(uuid, username, serverName, skin);

            this.bots.put(uuid, bot);
            this.onJoin.invoke(feature, bot);
        });

        this.proxyServer.getScheduler()
                .buildTask(this.plugin, this::refresh)
                .delay(Duration.ofSeconds(Sessions.RESYNC_SECS))
                .schedule();
    }

    public void removeBot(UUID uuid) {
        final var bot = this.bots.remove(uuid);

        if (bot == null) {
            return;
        }

        this.dispatch(feature -> this.onQuit.invoke(feature, bot));
    }

    public void refresh() {
        if (this.bots.isEmpty()) {
            return;
        }

        this.dispatch(feature -> {
            for (final var bot : this.bots.values()) {
                this.onJoin.invoke(feature, bot);
            }
        });
    }

    private void dispatch(Call call) {
        final var feature = this.globalPlayerList();

        if (feature == null) {
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
            this.logger.warn("TAB rejected an unplugged player's tab list entry.", exception);
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
