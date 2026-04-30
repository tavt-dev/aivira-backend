package com.tien.aivirabackend.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {
    private static final Pattern NON_LATIN_SLUG_CHAR = Pattern.compile("[^a-z0-9]+");
    private static final Pattern LEADING_TRAILING_DASH = Pattern.compile("(^-+|-+$)");

    private SlugUtils() {}

    public static String slugify(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = NON_LATIN_SLUG_CHAR.matcher(normalized).replaceAll("-");
        slug = LEADING_TRAILING_DASH.matcher(slug).replaceAll("");
        return slug.isBlank() ? fallback : slug;
    }
}
