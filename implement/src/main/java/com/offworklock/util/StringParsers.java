package com.offworklock.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small helper methods for parsing primitive types and collections from configuration strings.
 */
public final class StringParsers {
    private StringParsers() {
    }

    public static List<String> splitList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = value.split(",");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public static int parseInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double parseDouble(String value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(trimmed) || "yes".equalsIgnoreCase(trimmed) || "on".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed) || "no".equalsIgnoreCase(trimmed) || "off".equalsIgnoreCase(trimmed)) {
            return false;
        }
        return defaultValue;
    }

    public static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
