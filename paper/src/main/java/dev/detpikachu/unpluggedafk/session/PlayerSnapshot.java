package dev.detpikachu.unpluggedafk.session;

import dev.detpikachu.unpluggedafk.common.logging.Log;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class PlayerSnapshot {

    public static CompoundTag capture(Player player) {
        final var handle = ((CraftPlayer) player).getHandle();

        try (final var reporter = new ProblemReporter.ScopedCollector(handle.problemPath(), Log.logger())) {
            final var output = TagValueOutput.createWithContext(reporter, handle.registryAccess());

            handle.saveWithoutId(output);

            return output.buildResult();
        }
    }
}
