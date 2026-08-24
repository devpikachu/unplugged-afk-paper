/**
 * Events fired by Unplugged AFK.
 *
 * <p>Register for these like any Bukkit event, with an {@code @EventHandler} method on a registered
 * {@link org.bukkit.event.Listener}. Declare Unplugged AFK under {@code softdepend} in your {@code plugin.yml} so it
 * has enabled first. If it is not installed, none of these ever fire, and your listener simply never runs.
 *
 * <p>{@link dev.detpikachu.unpluggedafk.api.events.PlayerUnplugEvent} is the only cancellable one, and the only way to
 * refuse an unplug. The other two are notifications after the fact.
 *
 * <p>All three are fired on the main server thread and are never asynchronous.
 *
 * <p>Nothing in this package references server internals, so a plugin compiling against it does not inherit Unplugged
 * AFK's exact-Minecraft-version requirement.
 */
@NullMarked
package dev.detpikachu.unpluggedafk.api.events;

import org.jspecify.annotations.NullMarked;
