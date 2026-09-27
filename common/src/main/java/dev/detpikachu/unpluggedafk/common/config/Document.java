package dev.detpikachu.unpluggedafk.common.config;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class Document {

    private static final String SEPARATOR = ".";

    private final Map<String, Object> values;

    private Document(Map<String, Object> values) {
        this.values = values;
    }

    public static Document of(Map<?, ?> raw) {
        final var values = new LinkedHashMap<String, Object>();

        raw.forEach((key, value) -> {
            if (value != null) {
                values.put(String.valueOf(key), value instanceof Map<?, ?> nested ? of(nested) : value);
            }
        });

        return new Document(values);
    }

    public @Nullable Object get(String path) {
        final var parent = this.parentOf(path, false);
        return parent == null ? null : parent.values.get(leafOf(path));
    }

    public @Nullable Document section(String path) {
        return this.get(path) instanceof Document section ? section : null;
    }

    public void rename(String path, String key) {
        if (key.contains(SEPARATOR)) {
            throw new IllegalArgumentException("A key cannot contain " + SEPARATOR + ": " + key);
        }

        final var parent = this.parentOf(path, false);

        if (parent == null || !parent.values.containsKey(leafOf(path))) {
            return;
        }

        parent.values.put(key, parent.values.remove(leafOf(path)));
    }

    public void move(String from, String to) {
        final var value = this.get(from);

        if (value == null) {
            return;
        }

        final var parent = this.parentOf(to, true);

        if (parent == null) {
            return;
        }

        this.remove(from);
        parent.values.put(leafOf(to), value);
    }

    public void remove(String path) {
        final var parent = this.parentOf(path, false);

        if (parent != null) {
            parent.values.remove(leafOf(path));
        }
    }

    public Map<String, Object> toMap() {
        final var map = new LinkedHashMap<String, Object>();

        this.values.forEach((key, value) -> map.put(key, value instanceof Document section ? section.toMap() : value));

        return map;
    }

    private @Nullable Document parentOf(String path, boolean create) {
        final var separator = path.lastIndexOf(SEPARATOR);

        if (separator < 0) {
            return this;
        }

        var current = this;
        var start = 0;

        while (start <= separator) {
            final var end = path.indexOf(SEPARATOR, start);
            final var key = path.substring(start, end);
            final var next = current.values.get(key);

            start = end + 1;

            if (next instanceof Document section) {
                current = section;
                continue;
            }

            if (!create || next != null) {
                return null;
            }

            final var section = new Document(new LinkedHashMap<>());
            current.values.put(key, section);
            current = section;
        }

        return current;
    }

    private static String leafOf(String path) {
        return path.substring(path.lastIndexOf(SEPARATOR) + 1);
    }
}
