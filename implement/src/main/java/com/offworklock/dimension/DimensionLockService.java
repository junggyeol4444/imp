package com.offworklock.dimension;

import com.offworklock.config.ConfigData;
import com.offworklock.config.ConfigManager;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Determines whether a given dimension is subject to the off-work lock.
 *
 * Uses a simple cached view of the configured lockedDimensions. The cache is
 * refreshed automatically when the configuration changes (detected via a
 * lightweight version hash).
 */
public final class DimensionLockService {

    private final ConfigManager configManager;
    private final Set<String> cachedLockedDimensions = new HashSet<>();
    private long version = -1L;

    public DimensionLockService(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        // Initialize cache from current config.
        ConfigData config = Objects.requireNonNull(configManager.getConfig(), "config");
        refreshCache(config);
    }

    /**
     * Returns whether the given dimension id is configured as locked.
     *
     * @param dimensionId dimension identifier (e.g. "minecraft:overworld")
     */
    public synchronized boolean isLocked(String dimensionId) {
        ensureCacheUpToDate();
        return cachedLockedDimensions.contains(normalize(dimensionId));
    }

    /**
     * Ensures the in-memory cache reflects the latest configuration.
     * Called from synchronized entry points.
     */
    private synchronized void ensureCacheUpToDate() {
        ConfigData config = Objects.requireNonNull(configManager.getConfig(), "config");
        long newVersion = computeVersion(config);
        if (newVersion != this.version) {
            refreshCache(config);
        }
    }

    /**
     * Rebuilds the cached locked dimension set from the given config.
     * Must be called with synchronization handled by the caller.
     */
    private void refreshCache(ConfigData config) {
        cachedLockedDimensions.clear();
        for (String dimension : config.getLockedDimensions()) {
            String normalized = normalize(dimension);
            if (!normalized.isEmpty()) {
                cachedLockedDimensions.add(normalized);
            }
        }
        this.version = computeVersion(config);
    }

    /**
     * Computes a simple version hash based on the lockedDimensions list.
     */
    private long computeVersion(ConfigData config) {
        return Objects.requireNonNull(config.getLockedDimensions(), "lockedDimensions").hashCode();
    }

    /**
     * Normalizes dimension identifiers for comparison.
     */
    private String normalize(String dimensionId) {
        return (dimensionId == null)
                ? ""
                : dimensionId.trim().toLowerCase();
    }
}
