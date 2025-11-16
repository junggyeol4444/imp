package com.offworklock.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralizes filesystem locations used by the mod so both the actual mod runtime and tests/tools
 * can agree on where configuration and player data lives.
 */
public final class OffWorkLockPaths {
    private OffWorkLockPaths() {
    }

    public static Path defaultConfigDirectory() {
        return Paths.get("config");
    }

    public static Path defaultPlayerDataDirectory() {
        return Paths.get("playerdata");
    }
}
