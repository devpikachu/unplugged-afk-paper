package dev.detpikachu.unpluggedafk.formatting;

import dev.detpikachu.unpluggedafk.player.UnpluggedSession;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public final class UnpluggedDumpFormatting {

    private static final String EMPTY_SECTION = "(empty)";
    private static final String INDENT = "  ";
    private static final int LABEL_WIDTH = 12;
    private static final long MILLIS_PER_MINUTE = 60_000L;

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    public static String formatDump(Player player, UnpluggedSession session, Instant timestamp) {
        final var location = player.getLocation();
        final var inventory = player.getInventory();
        final var builder = new StringBuilder();

        appendField(builder, "Timestamp", timestamp.toEpochMilli() + " (" + TIMESTAMP_FORMAT.format(timestamp) + ")");
        appendField(builder, "Player", player.getName() + " (" + player.getUniqueId() + ")");
        appendField(builder, "Duration", session.durationMins() + " minute(s)");
        appendField(builder, "Expires", TIMESTAMP_FORMAT.format(timestamp.plusMillis(session.durationMins() * MILLIS_PER_MINUTE)));
        appendField(builder, "Reason", session.reason());
        appendField(builder, "World", player.getWorld().getName() + " (" + player.getWorld().getKey().asString() + ")");
        appendField(builder, "Position", format("%.2f, %.2f, %.2f", location.getX(), location.getY(), location.getZ()));
        appendField(builder, "Rotation", format("yaw %.2f, pitch %.2f", location.getYaw(), location.getPitch()));
        appendField(builder, "Game mode", player.getGameMode().name());
        appendField(builder, "Health", format("%.1f", player.getHealth()));
        appendField(builder, "Food", player.getFoodLevel() + format(" (saturation %.1f)", player.getSaturation()));
        appendField(builder, "Experience", "level " + player.getLevel() + " (" + player.getTotalExperience() + " total)");

        appendSection(builder, "Inventory", inventory.getStorageContents());
        appendSection(builder, "Armor", inventory.getArmorContents());
        appendSection(builder, "Off hand", new ItemStack[]{inventory.getItemInOffHand()});
        appendSection(builder, "Ender chest", player.getEnderChest().getContents());

        return builder.toString();
    }

    private static void appendField(StringBuilder builder, String label, String value) {
        builder.append(label)
                .append(':')
                .repeat(" ", Math.max(1, LABEL_WIDTH - label.length()))
                .append(value)
                .append('\n');
    }

    private static void appendSection(StringBuilder builder, String title, ItemStack[] contents) {
        builder.append('\n').append(title).append('\n').repeat("-", title.length()).append('\n');

        var empty = true;

        for (var slot = 0; slot < contents.length; slot++) {
            final var item = contents[slot];

            if (item == null || item.getType().isAir()) {
                continue;
            }

            empty = false;
            appendItem(builder, slot, item, "");
        }

        if (empty) {
            builder.append(EMPTY_SECTION).append('\n');
        }
    }

    private static void appendItem(StringBuilder builder, int slot, ItemStack item, String indent) {
        builder.append(indent).append('[').append(slot).append("] ")
                .append(item.getType().getKey().asString()).append(" x").append(item.getAmount());

        if (!item.hasItemMeta()) {
            builder.append('\n');
            return;
        }

        final var meta = item.getItemMeta();

        if (meta.hasDisplayName()) {
            builder.append(" \"").append(PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()))).append('"');
        }

        if (meta instanceof Damageable damageable && damageable.hasDamage()) {
            builder.append(" (damage ").append(damageable.getDamage()).append('/').append(item.getType().getMaxDurability()).append(')');
        }

        builder.append('\n');

        appendEnchantments(builder, "Enchantments", item.getEnchantments(), indent + INDENT);

        if (meta instanceof EnchantmentStorageMeta storageMeta && storageMeta.hasStoredEnchants()) {
            appendEnchantments(builder, "Stored enchantments", storageMeta.getStoredEnchants(), indent + INDENT);
        }

        appendNestedItems(builder, meta, indent + INDENT);
    }

    private static void appendEnchantments(StringBuilder builder, String title, Map<Enchantment, Integer> enchantments, String indent) {
        if (enchantments.isEmpty()) {
            return;
        }

        builder.append(indent).append(title).append(':').append('\n');

        enchantments.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getKey().asString()))
                .forEach(entry -> builder.append(indent).append(INDENT)
                        .append(entry.getKey().getKey().asString()).append(' ').append(entry.getValue()).append('\n'));
    }

    private static void appendNestedItems(StringBuilder builder, ItemMeta meta, String indent) {
        if (meta instanceof BlockStateMeta blockStateMeta
                && blockStateMeta.hasBlockState()
                && blockStateMeta.getBlockState() instanceof Container container) {
            appendNestedSection(builder, "Contents", container.getInventory().getContents(), indent);
            return;
        }

        if (meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
            appendNestedSection(builder, "Bundled", bundleMeta.getItems().toArray(ItemStack[]::new), indent);
        }
    }

    private static void appendNestedSection(StringBuilder builder, String title, ItemStack[] contents, String indent) {
        var empty = true;

        for (var slot = 0; slot < contents.length; slot++) {
            final var item = contents[slot];

            if (item == null || item.getType().isAir()) {
                continue;
            }

            if (empty) {
                builder.append(indent).append(title).append(':').append('\n');
                empty = false;
            }

            appendItem(builder, slot, item, indent + INDENT);
        }
    }

    private static String format(String pattern, Object... arguments) {
        return String.format(Locale.ROOT, pattern, arguments);
    }
}
