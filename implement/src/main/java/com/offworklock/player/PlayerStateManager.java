package com.offworklock.player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles loading and storing per-player state for a specific world or multiplayer server.
 */
public final class PlayerStateManager {

    private final Path storageDirectory;
    private final Map<String, Map<UUID, PlayerState>> cache = new HashMap<>();

    public PlayerStateManager(Path storageDirectory) throws IOException {
        Objects.requireNonNull(storageDirectory, "storageDirectory");
        this.storageDirectory = storageDirectory;
        Files.createDirectories(storageDirectory);
    }

    public synchronized PlayerState getOrCreateState(String contextId, UUID playerId) throws IOException {
        Map<UUID, PlayerState> contextStates = loadContext(contextId);
        return contextStates.computeIfAbsent(playerId, ignored -> new PlayerState(0, false));
    }

    public synchronized void updateState(String contextId, UUID playerId, PlayerState newState) throws IOException {
        Objects.requireNonNull(newState, "newState");
        Map<UUID, PlayerState> contextStates = loadContext(contextId);
        contextStates.put(playerId, newState);
        saveContext(contextId, contextStates);
    }

    public synchronized Optional<PlayerState> findState(String contextId, UUID playerId) throws IOException {
        Map<UUID, PlayerState> contextStates = loadContext(contextId);
        return Optional.ofNullable(contextStates.get(playerId));
    }

    public synchronized Map<UUID, PlayerState> snapshotContext(String contextId) throws IOException {
        return Collections.unmodifiableMap(new LinkedHashMap<>(loadContext(contextId)));
    }

    private Map<UUID, PlayerState> loadContext(String contextId) throws IOException {
        String safeContext = sanitizeContextId(contextId);
        Map<UUID, PlayerState> contextStates = cache.get(safeContext);
        if (contextStates != null) {
            return contextStates;
        }
        Path file = storageDirectory.resolve(safeContext + ".dat");
        Map<UUID, PlayerState> loaded = new LinkedHashMap<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split(",");
                if (parts.length < 3) {
                    continue;
                }
                try {
                    UUID playerId = UUID.fromString(parts[0].trim());
                    int points = Integer.parseInt(parts[1].trim());
                    boolean canOffWork = Boolean.parseBoolean(parts[2].trim());
                    boolean sessionOpen = parts.length > 3 && Boolean.parseBoolean(parts[3].trim());
                    int forcedExitCount = 0;
                    if (parts.length > 4) {
                        try {
                            forcedExitCount = Integer.parseInt(parts[4].trim());
                        } catch (NumberFormatException ignored) {
                            forcedExitCount = 0;
                        }
                    }
                    loaded.put(playerId, new PlayerState(points, canOffWork, sessionOpen, forcedExitCount));
                } catch (IllegalArgumentException ex) {
                    // Skip malformed entries.
                }
            }
        }
        cache.put(safeContext, loaded);
        return loaded;
    }

    private void saveContext(String contextId, Map<UUID, PlayerState> states) throws IOException {
        String safeContext = sanitizeContextId(contextId);
        Path file = storageDirectory.resolve(safeContext + ".dat");
        StringBuilder builder = new StringBuilder();
        builder.append("# Player state for context ").append(contextId).append('\n');
        for (Map.Entry<UUID, PlayerState> entry : states.entrySet()) {
            PlayerState state = entry.getValue();
            builder.append(entry.getKey())
                    .append(',')
                    .append(state.getPoints())
                    .append(',')
                    .append(state.canOffWork())
                    .append(',')
                    .append(state.isSessionOpen())
                    .append(',')
                    .append(state.getForcedExitCount())
                    .append('\n');
        }
        Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
    }

    private String sanitizeContextId(String contextId) {
        String value = contextId == null ? "default" : contextId.trim();
        if (value.isEmpty()) {
            value = "default";
        }
        return value.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
