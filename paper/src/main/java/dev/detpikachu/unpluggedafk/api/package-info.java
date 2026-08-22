/**
 * Public API for reading unplugged-player state from Unplugged AFK.
 *
 * <p>Obtain an {@link dev.detpikachu.unpluggedafk.api.UnpluggedAfkApi} through Bukkit's services manager rather than
 * by constructing one:
 *
 * <pre>{@code
 * var registration = Bukkit.getServicesManager().getRegistration(UnpluggedAfkApi.class);
 * if (registration != null) {
 *     UnpluggedAfkApi api = registration.getProvider();
 * }
 * }</pre>
 *
 * <p>A null registration means Unplugged AFK is absent or disabled, which callers should treat as "nobody is
 * unplugged" rather than as an error. Declaring Unplugged AFK under {@code softdepend} in your {@code plugin.yml}
 * guarantees it has enabled first if it is installed at all.
 *
 * <p>Nothing in this package references server internals, so a plugin compiling against it does not inherit
 * Unplugged AFK's exact-Minecraft-version requirement.
 */
@NullMarked
package dev.detpikachu.unpluggedafk.api;

import org.jspecify.annotations.NullMarked;
