package dev.detpikachu.unpluggedafk.compat.miniplaceholders;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import io.github.miniplaceholders.api.MiniPlaceholders;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

/**
 * Publishes bot state to MiniPlaceholders on the backend.
 *
 * <p>Verified against MiniPlaceholders 3.1.0 for Paper, sha256
 * {@code 86c9ecd88c63e655f5cdff28d9dd5d9d422559338a816b032c986ff442459dc9}. The API is pinned to 3.1.0 rather than the
 * newer 3.2.0 because 3.2.0 is compiled for Java 25, so a Java 21 server cannot load it at all, and the published
 * surface is byte-identical between the two.
 *
 * <p>This is the only compat here whose plugin ships a {@code paper-plugin.yml} rather than a {@code plugin.yml}. That
 * is safe: it declares an open classloader, which puts it in Paper's global classloader group, and a legacy plugin like
 * this one sees that group wholesale. The {@code softdepend} entry still produces a correct load-order edge across the
 * two descriptor types, and without it the presence check below would lose the race.
 *
 * <p>{@link #unregister} has no PlaceholderAPI equivalent and is not optional. Registering a name MiniPlaceholders
 * already holds throws rather than returning false, and nothing clears its static registry when a plugin disables, so
 * without the teardown a {@code /reload} both fails to enable this plugin and pins the previous classloader alive.
 *
 * <p>PlaceholderAPI's three-valued answer collapses to two here, which is why {@link MiniPlaceholdersExpansion} has no
 * unknown-key arm: an unregistered key never reaches the resolver at all, so "not mine" needs no representation.
 *
 * <p>Values are inserted as literal components rather than parsed text, because the session reason is whatever the
 * player typed after {@code /unplug} and re-parsing it would let them smuggle MiniMessage markup into anything that
 * renders these tags.
 */
@ApiStatus.Internal
public final class MiniPlaceholdersCompat {

    private static final String PLUGIN_NAME = "MiniPlaceholders";

    public static void register(UnpluggedAfk plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        MiniPlaceholdersExpansion.build(plugin).register();

        LOGGER.info("MiniPlaceholders detected. Registering an expansion so other plugins can read bot state.");
    }

    public static void unregister(UnpluggedAfk plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        final var expansion = MiniPlaceholders.expansionByName(MiniPlaceholdersExpansion.IDENTIFIER);

        if (expansion != null) {
            expansion.unregister();
        }
    }
}
