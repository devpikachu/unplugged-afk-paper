package dev.detpikachu.unpluggedafk.velocity.compat.tab;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
final class TabStyle {

    private static final String LOCATOR_TAB_PLAYER = "me.neznamy.tab.shared.platform.TabPlayer";
    private static final String LOCATOR_PROPERTY = "me.neznamy.tab.shared.Property";
    private static final String LOCATOR_TABLIST_DATA =
            "me.neznamy.tab.shared.features.playerlist.TablistFormattingPlayerData";
    private static final String LOCATOR_TEAM_DATA = "me.neznamy.tab.shared.features.nametags.NameTagPlayerData";
    private static final String LOCATOR_PLAYER_LIST = "me.neznamy.tab.shared.features.playerlist.PlayerList";
    private static final String LOCATOR_TAB_FORMAT =
            "me.neznamy.tab.shared.features.playerlist.PlayerListProxyPlayerData";
    private static final String LOCATOR_NAME_TAG = "me.neznamy.tab.shared.features.nametags.NameTag";
    private static final String LOCATOR_NAMETAG = "me.neznamy.tab.shared.features.nametags.NameTagProxyPlayerData";
    private static final String LOCATOR_NAME_VISIBILITY = "me.neznamy.tab.shared.platform.Scoreboard$NameVisibility";
    private static final String LOCATOR_TAB_COMPONENT = "me.neznamy.tab.shared.chat.component.TabComponent";
    private static final String LOCATOR_CACHE = "me.neznamy.tab.shared.util.cache.Cache";
    private static final String LOCATOR_PROXY_PLAYER = "me.neznamy.tab.shared.features.proxy.ProxyPlayer";

    private final Field tablistData;
    private final Field tablistPrefix;
    private final Field tablistName;
    private final Field tablistSuffix;
    private final Field tablistDisabled;
    private final Field teamData;
    private final Field teamName;
    private final Field teamPrefix;
    private final Field teamSuffix;
    private final Method teamDisabled;
    private final Method teamVisibility;
    private final Method propertyValue;
    private final Method getCache;
    private final Method cacheGet;
    private final Constructor<?> newTabFormat;
    private final Constructor<?> newNametag;
    private final Method checkTeamName;
    private final Field resolvedTeamName;
    private final Object visible;
    private final Object hidden;
    private final Method setTabFormat;
    private final Method setNametag;
    private final Method formatForEveryone;
    private final Method unregisterTeam;
    private final Method registerTeam;

    private TabStyle() throws ReflectiveOperationException {
        final var tabPlayerClass = Class.forName(LOCATOR_TAB_PLAYER);
        final var propertyClass = Class.forName(LOCATOR_PROPERTY);
        final var tablistDataClass = Class.forName(LOCATOR_TABLIST_DATA);
        final var teamDataClass = Class.forName(LOCATOR_TEAM_DATA);
        final var playerListClass = Class.forName(LOCATOR_PLAYER_LIST);
        final var tabFormatClass = Class.forName(LOCATOR_TAB_FORMAT);
        final var nameTagClass = Class.forName(LOCATOR_NAME_TAG);
        final var nametagClass = Class.forName(LOCATOR_NAMETAG);
        final var nameVisibilityClass = Class.forName(LOCATOR_NAME_VISIBILITY);
        final var tabComponentClass = Class.forName(LOCATOR_TAB_COMPONENT);
        final var cacheClass = Class.forName(LOCATOR_CACHE);
        final var proxyPlayerClass = Class.forName(LOCATOR_PROXY_PLAYER);

        this.tablistData = tabPlayerClass.getField("tablistData");
        this.tablistPrefix = tablistDataClass.getField("prefix");
        this.tablistName = tablistDataClass.getField("name");
        this.tablistSuffix = tablistDataClass.getField("suffix");
        this.tablistDisabled = tablistDataClass.getField("disabled");
        this.teamData = tabPlayerClass.getField("teamData");
        this.teamName = teamDataClass.getField("teamName");
        this.teamPrefix = teamDataClass.getField("prefix");
        this.teamSuffix = teamDataClass.getField("suffix");
        this.teamDisabled = teamDataClass.getMethod("isDisabled");
        this.teamVisibility = teamDataClass.getMethod("getTeamVisibility", tabPlayerClass);
        this.propertyValue = propertyClass.getMethod("get");
        this.getCache = playerListClass.getMethod("getCache");
        this.cacheGet = cacheClass.getMethod("get", Object.class);
        this.newTabFormat = tabFormatClass.getConstructor(
                playerListClass,
                long.class,
                UUID.class,
                String.class,
                String.class,
                String.class,
                String.class,
                tabComponentClass,
                boolean.class);
        this.newNametag = nametagClass.getConstructor(
                nameTagClass,
                long.class,
                UUID.class,
                String.class,
                String.class,
                String.class,
                nameVisibilityClass,
                boolean.class);
        this.checkTeamName = nametagClass.getDeclaredMethod("checkTeamName", proxyPlayerClass, String.class);
        this.checkTeamName.setAccessible(true);
        this.resolvedTeamName = nametagClass.getDeclaredField("resolvedTeamName");
        this.resolvedTeamName.setAccessible(true);
        this.visible = nameVisibilityClass.getField("ALWAYS").get(null);
        this.hidden = nameVisibilityClass.getField("NEVER").get(null);
        this.setTabFormat = proxyPlayerClass.getMethod("setTabFormat", tabFormatClass);
        this.setNametag = proxyPlayerClass.getMethod("setNametag", nametagClass);
        this.formatForEveryone = playerListClass.getMethod("formatPlayerForEveryone", proxyPlayerClass);
        this.unregisterTeam = nameTagClass.getMethod("unregisterTeam", proxyPlayerClass);
        this.registerTeam = nameTagClass.getMethod("onJoin", proxyPlayerClass);
    }

    static @Nullable TabStyle resolve() {
        try {
            return new TabStyle();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Log.warn(
                    "Could not resolve TAB's formatting internals. Bots keep their plain name and sort first.",
                    exception);
            return null;
        }
    }

    Look capture(Object player, UUID uuid, String username, @Nullable Object playerList, @Nullable Object nameTag)
            throws ReflectiveOperationException {
        return new Look(
                playerList == null ? null : this.tabFormatOf(player, uuid, username, playerList),
                nameTag == null ? null : this.nametagOf(player, uuid, nameTag));
    }

    void attach(Object bot, Look look) throws ReflectiveOperationException {
        if (look.tabFormat() != null) {
            this.setTabFormat.invoke(bot, look.tabFormat());
        }

        if (look.nametag() != null) {
            this.setNametag.invoke(bot, look.nametag());
        }
    }

    void format(Object playerList, Object bot) throws ReflectiveOperationException {
        this.formatForEveryone.invoke(playerList, bot);
    }

    void registerTeam(Object nameTag, Object bot) throws ReflectiveOperationException {
        this.unregisterTeam.invoke(nameTag, bot);
        this.registerTeam.invoke(nameTag, bot);
    }

    void unregisterTeam(Object nameTag, Object bot) throws ReflectiveOperationException {
        this.unregisterTeam.invoke(nameTag, bot);
    }

    private @Nullable Object tabFormatOf(Object player, UUID uuid, String username, Object playerList)
            throws ReflectiveOperationException {
        final var data = this.tablistData.get(player);

        if (data == null || ((AtomicBoolean) this.tablistDisabled.get(data)).get()) {
            return null;
        }

        final var prefix = this.valueOf(this.tablistPrefix.get(data));
        final var name = this.valueOf(this.tablistName.get(data));
        final var suffix = this.valueOf(this.tablistSuffix.get(data));

        if (prefix == null || name == null || suffix == null) {
            return null;
        }

        final var component = this.cacheGet.invoke(this.getCache.invoke(playerList), prefix + name + suffix);
        return this.newTabFormat.newInstance(playerList, 0L, uuid, username, prefix, name, suffix, component, false);
    }

    private @Nullable Object nametagOf(Object player, UUID uuid, Object nameTag) throws ReflectiveOperationException {
        final var data = this.teamData.get(player);

        if (data == null || (boolean) this.teamDisabled.invoke(data)) {
            return null;
        }

        final var name = (String) this.teamName.get(data);
        final var prefix = this.valueOf(this.teamPrefix.get(data));
        final var suffix = this.valueOf(this.teamSuffix.get(data));

        if (name == null || name.isEmpty() || prefix == null || suffix == null) {
            return null;
        }

        final var visibility = (boolean) this.teamVisibility.invoke(data, player) ? this.visible : this.hidden;
        final var nametag = this.newNametag.newInstance(nameTag, 0L, uuid, name, prefix, suffix, visibility, false);
        this.resolvedTeamName.set(
                nametag, this.checkTeamName.invoke(nametag, null, name.substring(0, name.length() - 1)));
        return nametag;
    }

    private @Nullable String valueOf(@Nullable Object property) throws ReflectiveOperationException {
        return property == null ? null : (String) this.propertyValue.invoke(property);
    }

    record Look(@Nullable Object tabFormat, @Nullable Object nametag) {}
}
