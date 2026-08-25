package dev.detpikachu.unpluggedafk.compat.luckperms;

import dev.detpikachu.unpluggedafk.player.UnpluggedServerPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;
import static dev.detpikachu.unpluggedafk.UnpluggedAfk.logDebug;

@ApiStatus.Internal
public final class LuckPermsBridge {

    private static final String LOCATOR_API_PROVIDER = "me.lucko.luckperms.common.api.LuckPermsApiProvider";
    private static final String LOCATOR_API_USER = "me.lucko.luckperms.common.api.implementation.ApiUser";
    private static final String LOCATOR_BUKKIT_PLUGIN = "me.lucko.luckperms.bukkit.LPBukkitPlugin";
    private static final String LOCATOR_MODEL_USER = "me.lucko.luckperms.common.model.User";
    private static final String LOCATOR_PERMISSIBLE =
            "me.lucko.luckperms.bukkit.inject.permissible.LuckPermsPermissible";
    private static final String LOCATOR_PERMISSIBLE_INJECTOR =
            "me.lucko.luckperms.bukkit.inject.permissible.PermissibleInjector";
    private static final String LOCATOR_PLUGIN_LOGGER = "me.lucko.luckperms.common.plugin.logging.PluginLogger";

    private static final String PLUGIN_FIELD = "plugin";

    private final LuckPerms api;
    private final Object luckPermsPlugin;

    private final Method cast;
    private final Method getLogger;
    private final Constructor<?> permissible;
    private final Method inject;

    private LuckPermsBridge(LuckPerms api) throws ReflectiveOperationException {
        final var loader = api.getClass().getClassLoader();

        final var apiProviderClass = Class.forName(LOCATOR_API_PROVIDER, true, loader);
        final var apiUserClass = Class.forName(LOCATOR_API_USER, true, loader);
        final var bukkitPluginClass = Class.forName(LOCATOR_BUKKIT_PLUGIN, true, loader);
        final var modelUserClass = Class.forName(LOCATOR_MODEL_USER, true, loader);
        final var permissibleClass = Class.forName(LOCATOR_PERMISSIBLE, true, loader);
        final var injectorClass = Class.forName(LOCATOR_PERMISSIBLE_INJECTOR, true, loader);
        final var pluginLoggerClass = Class.forName(LOCATOR_PLUGIN_LOGGER, true, loader);

        final var pluginField = apiProviderClass.getDeclaredField(PLUGIN_FIELD);
        pluginField.setAccessible(true);

        this.api = api;
        this.luckPermsPlugin = Objects.requireNonNull(pluginField.get(api), PLUGIN_FIELD);

        this.cast = apiUserClass.getMethod("cast", User.class);
        this.getLogger = bukkitPluginClass.getMethod("getLogger");
        this.permissible = permissibleClass.getConstructor(Player.class, modelUserClass, bukkitPluginClass);
        this.inject = injectorClass.getMethod("inject", Player.class, permissibleClass, pluginLoggerClass);
    }

    static @Nullable LuckPermsBridge resolve() {
        try {
            return new LuckPermsBridge(LuckPermsProvider.get());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Could not resolve LuckPerms' internals. Bots will resolve permissions without it.", exception);
            return null;
        }
    }

    void attach(UnpluggedServerPlayer bot) {
        final var user = this.api.getUserManager().getUser(bot.getUUID());

        if (user == null) {
            LOGGER.warn(
                    "LuckPerms holds no loaded user for bot {}, so it keeps the vanilla permissible.", bot.describe());
            return;
        }

        try {
            final var player = bot.getBukkitEntity();

            this.inject.invoke(
                    null,
                    player,
                    this.permissible.newInstance(player, this.cast.invoke(null, user), this.luckPermsPlugin),
                    this.getLogger.invoke(this.luckPermsPlugin));
            logDebug("Attached LuckPerms to bot {}.", bot.describe());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("LuckPerms rejected an injection into bot {}.", bot.describe(), exception);
        }
    }
}
