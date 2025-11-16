package com.offworklock.gacha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Parsed representation of an effect command defined in the configuration.
 *
 * Effect syntax (examples):
 *  - unlock_exit
 *  - add_points:50
 *  - add_currency:1000
 *  - give_item:minecraft:diamond:1
 *  - give_effect:haste:120:1
 *  - message:퇴근 축하합니다!
 *  - any_custom_payload...
 *
 * The first segment (before the first ':') is treated as the effect keyword.
 * The remaining segments are passed through as ordered arguments.
 */
public final class GachaEffect {

    private final String rawCommand;
    private final Type type;
    private final List<String> arguments;

    private GachaEffect(String rawCommand, Type type, List<String> arguments) {
        this.rawCommand = Objects.requireNonNull(rawCommand, "rawCommand");
        this.type = Objects.requireNonNull(type, "type");
        this.arguments = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(arguments, "arguments")));
    }

    /**
     * Parses a raw effect command string into a {@link GachaEffect}.
     * <ul>
     *     <li>Null or blank input becomes a {@code CUSTOM} effect with no arguments.</li>
     *     <li>The first ':'-separated token (lowercased) determines the {@link Type}.</li>
     *     <li>Remaining tokens are stored as arguments in order (trimmed, may be empty).</li>
     * </ul>
     */
    public static GachaEffect parse(String command) {
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.isEmpty()) {
            return new GachaEffect("", Type.CUSTOM, List.of());
        }

        // Keep all segments; the first is the keyword, the rest are arguments.
        String[] parts = trimmed.split(":", -1);
        String keyword = parts[0].trim().toLowerCase(Locale.ROOT);

        Type type = Type.fromKeyword(keyword);
        List<String> args = new ArrayList<>(Math.max(0, parts.length - 1));

        for (int i = 1; i < parts.length; i++) {
            // Preserve position semantics but normalize whitespace.
            args.add(parts[i].trim());
        }

        return new GachaEffect(trimmed, type, args);
    }

    /**
     * Returns the original, unmodified command string.
     */
    public String getRawCommand() {
        return rawCommand;
    }

    /**
     * Returns the resolved effect type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns an immutable list of parsed arguments (may be empty).
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Known effect types. Anything unrecognized falls back to {@link #CUSTOM}.
     */
    public enum Type {
        UNLOCK_EXIT("unlock_exit"),
        ADD_POINTS("add_points"),
        ADD_CURRENCY("add_currency"),
        GIVE_ITEM("give_item"),
        GIVE_EFFECT("give_effect"),
        MESSAGE("message"),
        CUSTOM("custom");

        private final String keyword;

        Type(String keyword) {
            this.keyword = keyword;
        }

        static Type fromKeyword(String keyword) {
            if (keyword == null || keyword.isEmpty()) {
                return CUSTOM;
            }
            String lower = keyword.toLowerCase(Locale.ROOT);
            for (Type type : values()) {
                if (type.keyword.equals(lower)) {
                    return type;
                }
            }
            return CUSTOM;
        }
    }
}
