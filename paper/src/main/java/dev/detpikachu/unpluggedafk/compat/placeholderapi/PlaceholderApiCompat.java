package dev.detpikachu.unpluggedafk.compat.placeholderapi;

import dev.detpikachu.unpluggedafk.UnpluggedAfk;
import org.jetbrains.annotations.ApiStatus;

import static dev.detpikachu.unpluggedafk.UnpluggedAfk.LOGGER;

/**
 * Publishes session state to PlaceholderAPI as the {@code unplugged-afk} expansion.
 *
 * <p>{@link PlaceholderApiExpansion#persist()} has to return {@code true}. An expansion registered from inside a plugin
 * is unregistered for good on a PlaceholderAPI reload otherwise, because nothing re-registers it until the server
 * restarts.
 *
 * <p>The two failure returns mean different things. {@code null} means the placeholder is not ours, and PlaceholderAPI
 * puts the raw text back so a typo stays visible. An empty string means the placeholder is ours but has no value, which
 * covers a player who is not unplugged and the null {@code OfflinePlayer} PlaceholderAPI passes when it resolves text
 * belonging to no player, such as a Discord channel topic.
 */
@ApiStatus.Internal
public final class PlaceholderApiCompat {

    private static final String PLUGIN_NAME = "PlaceholderAPI";

    public static void register(UnpluggedAfk plugin) {
        final var pluginManager = plugin.getServer().getPluginManager();

        if (!pluginManager.isPluginEnabled(PLUGIN_NAME)) {
            return;
        }

        if (!new PlaceholderApiExpansion(plugin).register()) {
            LOGGER.warn("PlaceholderAPI detected, but its expansion could not be registered. Placeholders are off.");
            return;
        }

        LOGGER.info("PlaceholderAPI detected. Registering an expansion so other plugins can read bot state.");
    }
}
