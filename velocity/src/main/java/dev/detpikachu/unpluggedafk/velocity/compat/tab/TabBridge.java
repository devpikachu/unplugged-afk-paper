package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.detpikachu.unpluggedafk.common.logging.Log;
import dev.detpikachu.unpluggedafk.velocity.UnpluggedAfkVelocity;
import dev.detpikachu.unpluggedafk.velocity.session.Session;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public final class TabBridge {

    private static final String GLOBAL_PLAYER_LIST = "GlobalPlayerList";
    private static final String TABLIST_FORMATTING = "TablistFormatting";
    private static final String NAME_TAGS = "NameTags";

    private static final int RESYNC_DELAY_SECS = 2;

    private static final String LOCATOR_TAB = "me.neznamy.tab.shared.TAB";
    private static final String LOCATOR_FEATURE_MANAGER = "me.neznamy.tab.shared.FeatureManager";
    private static final String LOCATOR_GLOBAL_PLAYER_LIST =
            "me.neznamy.tab.shared.features.globalplayerlist.GlobalPlayerList";
    private static final String LOCATOR_PROXY_PLAYER = "me.neznamy.tab.shared.features.proxy.ProxyPlayer";
    private static final String LOCATOR_SERVER = "me.neznamy.tab.shared.data.Server";
    private static final String LOCATOR_SKIN = "me.neznamy.tab.shared.platform.TabList$Skin";
    private static final String LOCATOR_THREAD_EXECUTOR = "me.neznamy.tab.shared.cpu.ThreadExecutor";
    private static final String LOCATOR_CUSTOM_THREADED = "me.neznamy.tab.shared.features.types.CustomThreaded";

    private final UnpluggedAfkVelocity plugin;
    private final ProxyServer proxyServer;

    private final Method getInstance;
    private final Method getPlayer;
    private final Method getFeatureManager;
    private final Method getFeature;
    private final Method getCustomThread;
    private final Method execute;
    private final Method onJoin;
    private final Method serverByName;
    private final Constructor<?> skin;
    private final Constructor<?> proxyPlayer;
    private final Method onQuit;
    private final @Nullable TabStyle style;

    private final ConcurrentHashMap<UUID, Object> bots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TabStyle.Look> looks = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshPending = new AtomicBoolean();

    private TabBridge(UnpluggedAfkVelocity plugin) throws ReflectiveOperationException {
        final var tabClass = Class.forName(LOCATOR_TAB);
        final var featureManagerClass = Class.forName(LOCATOR_FEATURE_MANAGER);
        final var globalPlayerListClass = Class.forName(LOCATOR_GLOBAL_PLAYER_LIST);
        final var proxyPlayerClass = Class.forName(LOCATOR_PROXY_PLAYER);
        final var serverClass = Class.forName(LOCATOR_SERVER);
        final var skinClass = Class.forName(LOCATOR_SKIN);
        final var threadExecutorClass = Class.forName(LOCATOR_THREAD_EXECUTOR);
        final var customThreadedClass = Class.forName(LOCATOR_CUSTOM_THREADED);

        this.plugin = plugin;
        this.proxyServer = plugin.getProxyServer();

        this.getInstance = tabClass.getMethod("getInstance");
        this.getPlayer = tabClass.getMethod("getPlayer", UUID.class);
        this.getFeatureManager = tabClass.getMethod("getFeatureManager");
        this.getFeature = featureManagerClass.getMethod("getFeature", String.class);
        this.getCustomThread = customThreadedClass.getMethod("getCustomThread");
        this.execute = threadExecutorClass.getMethod("execute", Runnable.class);
        this.onJoin = globalPlayerListClass.getMethod("onJoin", proxyPlayerClass);
        this.serverByName = serverClass.getMethod("byName", String.class);
        this.skin = skinClass.getConstructor(String.class, String.class);
        this.proxyPlayer = proxyPlayerClass.getConstructor(
                UUID.class, UUID.class, String.class, serverClass, boolean.class, boolean.class, skinClass);
        this.onQuit = globalPlayerListClass.getMethod("onQuit", proxyPlayerClass);
        this.style = TabStyle.resolve();
    }

    static @Nullable TabBridge resolve(UnpluggedAfkVelocity plugin) {
        try {
            return new TabBridge(plugin);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Log.warn("Could not resolve TAB's internals.", exception);
            return null;
        }
    }

    public Set<UUID> trackedBots() {
        return Set.copyOf(this.bots.keySet());
    }

    public void addBot(String serverName, UUID uuid, String username, Session.@Nullable Skin skin) {
        Log.debug("Building a TAB entry for bot {} ({}) on {}.", username, uuid, serverName);
        this.capture(uuid, username);
        this.dispatch(
                GLOBAL_PLAYER_LIST,
                feature -> this.bots.put(uuid, this.newProxyPlayer(serverName, uuid, username, skin)));
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
                    this.restyle();
                })
                .delay(Duration.ofSeconds(RESYNC_DELAY_SECS))
                .schedule();
    }

    public void removeBot(UUID uuid) {
        this.dispatch(GLOBAL_PLAYER_LIST, feature -> {
            final var bot = this.bots.remove(uuid);

            if (bot != null) {
                this.onQuit.invoke(feature, bot);
                this.leaveTeam(bot);
            }
        });
    }

    public void forget(UUID uuid) {
        this.looks.remove(uuid);
        this.removeBot(uuid);
    }

    public void refresh() {
        if (this.bots.isEmpty()) {
            return;
        }

        Log.debug("Re-asserting {} TAB entr(ies) to every viewer.", this.bots.size());
        this.dispatch(GLOBAL_PLAYER_LIST, feature -> {
            for (final var bot : this.bots.values()) {
                this.onJoin.invoke(feature, bot);
            }
        });
    }

    private void restyle() {
        final var style = this.style;

        if (style == null || this.bots.isEmpty()) {
            return;
        }

        this.dispatch(GLOBAL_PLAYER_LIST, feature -> {
            final var playerList = this.feature(TABLIST_FORMATTING);

            if (playerList != null) {
                for (final var bot : this.bots.values()) {
                    style.format(playerList, bot);
                }
            }
        });
        this.dispatch(NAME_TAGS, nameTag -> {
            for (final var bot : this.bots.values()) {
                style.registerTeam(nameTag, bot);
            }
        });
    }

    private void leaveTeam(Object bot) {
        final var style = this.style;

        if (style != null) {
            this.dispatch(NAME_TAGS, nameTag -> style.unregisterTeam(nameTag, bot));
        }
    }

    private void capture(UUID uuid, String username) {
        final var style = this.style;

        if (style == null) {
            return;
        }

        try {
            final var tab = this.getInstance.invoke(null);
            final var player = tab == null ? null : this.getPlayer.invoke(tab, uuid);

            if (player == null) {
                Log.debug("{} is not a TAB player, so bot {} keeps its last known TAB look.", username, uuid);
                return;
            }

            this.looks.put(
                    uuid,
                    style.capture(player, uuid, username, this.feature(TABLIST_FORMATTING), this.feature(NAME_TAGS)));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Log.warn("Could not read the TAB look of {}.", username, exception);
        }
    }

    private void dispatch(String featureName, Call call) {
        final var feature = this.feature(featureName);

        if (feature == null) {
            Log.debug("TAB has no {} feature, so it is skipped for bots.", featureName);
            return;
        }

        try {
            this.execute.invoke(this.getCustomThread.invoke(feature), (Runnable) () -> this.run(feature, call));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Log.warn("Could not reach TAB's feature thread.", exception);
        }
    }

    private void run(Object feature, Call call) {
        try {
            call.invoke(feature);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Log.warn("TAB rejected a bot's tab list entry.", exception);
        }
    }

    private @Nullable Object feature(String featureName) {
        try {
            final var tab = this.getInstance.invoke(null);

            if (tab == null) {
                return null;
            }

            return this.getFeature.invoke(this.getFeatureManager.invoke(tab), featureName);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Log.warn("Could not ask TAB for its {} feature.", featureName, exception);
            return null;
        }
    }

    private Object newProxyPlayer(String serverName, UUID uuid, String username, Session.@Nullable Skin skin)
            throws ReflectiveOperationException {
        final var server = this.serverByName.invoke(null, serverName);
        final var texture = skin == null ? null : this.skin.newInstance(skin.value(), skin.signature());
        final var bot = this.proxyPlayer.newInstance(uuid, uuid, username, server, false, false, texture);
        final var look = this.looks.get(uuid);

        if (this.style != null && look != null) {
            this.style.attach(bot, look);
        }

        return bot;
    }

    @FunctionalInterface
    private interface Call {

        void invoke(Object feature) throws ReflectiveOperationException;
    }
}
