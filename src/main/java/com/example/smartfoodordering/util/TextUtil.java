package com.example.smartfoodordering.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class TextUtil {
    private TextUtil() {}

    public static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    /** Splits "a, b ,c" into a trimmed, de-duplicated, ordered set; blank input gives an empty set. */
    public static Set<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return new LinkedHashSet<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> truncate(s, 60))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
