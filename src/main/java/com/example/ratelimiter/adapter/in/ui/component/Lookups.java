package com.example.ratelimiter.adapter.in.ui.component;

import java.util.Map;
import java.util.function.Function;

/** Null-safe label lookup for foreign keys shown in grids. */
public final class Lookups {

    private Lookups() {
    }

    public static <T> String label(Map<Long, T> items, Long id, Function<T, String> labeler) {
        if (id == null) {
            return "";
        }
        T item = items.get(id);
        return item == null ? "#" + id : labeler.apply(item);
    }
}
