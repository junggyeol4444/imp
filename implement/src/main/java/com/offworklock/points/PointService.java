package com.offworklock.points;

import com.offworklock.config.ConfigData;
import com.offworklock.config.ConfigManager;
import com.offworklock.player.PlayerState;
import com.offworklock.player.PlayerStateManager;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles point accumulation in both automatic and manual exchange modes.
 */
public final class PointService {

    private final ConfigManager configManager;
    private final PlayerStateManager playerStateManager;

    public PointService(ConfigManager configManager, PlayerStateManager playerStateManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.playerStateManager = Objects.requireNonNull(playerStateManager, "playerStateManager");
    }

    /**
     * Processes a mined block. Returns information about whether points were awarded, and whether the
     * original block drops should be suppressed (automatic mode).
     */
    public PointAwardResult handleBlockMined(String contextId, UUID playerId, String blockId) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(blockId, "blockId");

        ConfigData config = configManager.getConfig();
        Integer value = config.getOreValues().get(blockId);
        if (value == null || value <= 0) {
            return PointAwardResult.noMatch();
        }

        PlayerState state = playerStateManager.getOrCreateState(contextId, playerId);
        if (config.getPointAccumulationMode() == ConfigData.PointAccumulationMode.MANUAL) {
            // Manual mode keeps the normal drops and defers point conversion to exchange.
            return PointAwardResult.manualMatch(value, state.getPoints());
        }

        int current = Math.max(0, state.getPoints());
        long updated = (long) current + value;
        int newTotal = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, updated));

        PlayerState updatedState = state.withPoints(newTotal);
        playerStateManager.updateState(contextId, playerId, updatedState);

        return PointAwardResult.automaticAward(value, newTotal);
    }

    /**
     * Performs a manual ore exchange by scanning the provided inventory snapshot.
     *
     * @param inventoryCounts mapping of item identifiers to stack counts. Only ores defined in the configuration
     *                        will be consumed.
     */
    public ManualExchangeResult exchangeOres(String contextId,
                                             UUID playerId,
                                             Map<String, Integer> inventoryCounts) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(inventoryCounts, "inventoryCounts");

        ConfigData config = configManager.getConfig();
        if (config.getPointAccumulationMode() != ConfigData.PointAccumulationMode.MANUAL) {
            return ManualExchangeResult.ignored();
        }

        Map<String, Integer> consumed = new LinkedHashMap<>();
        long totalPoints = 0L;

        for (Map.Entry<String, Integer> entry : inventoryCounts.entrySet()) {
            String itemId = entry.getKey();
            if (itemId == null) {
                continue;
            }

            Integer perItemValue = config.getOreValues().get(itemId);
            if (perItemValue == null || perItemValue <= 0) {
                continue;
            }

            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count <= 0) {
                continue;
            }

            count = Math.max(0, count);
            consumed.put(itemId, count);
            totalPoints += (long) perItemValue * count;
        }

        if (totalPoints <= 0L) {
            return ManualExchangeResult.empty();
        }

        PlayerState state = playerStateManager.getOrCreateState(contextId, playerId);
        int current = Math.max(0, state.getPoints());
        long updated = (long) current + totalPoints;
        int newTotal = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, updated));

        PlayerState updatedState = state.withPoints(newTotal);
        playerStateManager.updateState(contextId, playerId, updatedState);

        return ManualExchangeResult.success((int) totalPoints, newTotal, consumed);
    }

    /**
     * Result of handling a block mined event.
     */
    public static final class PointAwardResult {
        private static final PointAwardResult NO_MATCH =
                new PointAwardResult(false, false, 0, 0, false, null);

        private final boolean matchedOre;
        private final boolean pointsChanged;
        private final int pointsAwarded;
        private final int totalPoints;
        private final boolean suppressBlockDrop;
        private final String notificationMessage;

        private PointAwardResult(boolean matchedOre,
                                 boolean pointsChanged,
                                 int pointsAwarded,
                                 int totalPoints,
                                 boolean suppressBlockDrop,
                                 String notificationMessage) {
            this.matchedOre = matchedOre;
            this.pointsChanged = pointsChanged;
            this.pointsAwarded = pointsAwarded;
            this.totalPoints = totalPoints;
            this.suppressBlockDrop = suppressBlockDrop;
            this.notificationMessage = notificationMessage;
        }

        public static PointAwardResult noMatch() {
            return NO_MATCH;
        }

        public static PointAwardResult manualMatch(int potentialPoints, int currentTotal) {
            String message = "+" + potentialPoints + " pts (교환 필요)";
            return new PointAwardResult(true, false, 0, currentTotal, false, message);
        }

        public static PointAwardResult automaticAward(int awardedPoints, int newTotal) {
            String message = "+" + awardedPoints + " pts";
            return new PointAwardResult(true, true, awardedPoints, newTotal, true, message);
        }

        public boolean matchedOre() {
            return matchedOre;
        }

        public boolean pointsChanged() {
            return pointsChanged;
        }

        public int getPointsAwarded() {
            return pointsAwarded;
        }

        public int getTotalPoints() {
            return totalPoints;
        }

        public boolean shouldSuppressBlockDrop() {
            return suppressBlockDrop;
        }

        public String getNotificationMessage() {
            return notificationMessage;
        }
    }

    /**
     * Result of performing a manual ore exchange.
     */
    public static final class ManualExchangeResult {
        private static final ManualExchangeResult IGNORED =
                new ManualExchangeResult(false, 0, 0, Collections.emptyMap(), null);
        private static final ManualExchangeResult EMPTY =
                new ManualExchangeResult(true, 0, 0, Collections.emptyMap(), "교환 가능한 광석이 없습니다.");

        private final boolean processed;
        private final int pointsGained;
        private final int totalPoints;
        private final Map<String, Integer> consumedItems;
        private final String notificationMessage;

        private ManualExchangeResult(boolean processed,
                                     int pointsGained,
                                     int totalPoints,
                                     Map<String, Integer> consumedItems,
                                     String notificationMessage) {
            this.processed = processed;
            this.pointsGained = pointsGained;
            this.totalPoints = totalPoints;
            this.consumedItems = Collections.unmodifiableMap(consumedItems);
            this.notificationMessage = notificationMessage;
        }

        public static ManualExchangeResult ignored() {
            return IGNORED;
        }

        public static ManualExchangeResult empty() {
            return EMPTY;
        }

        public static ManualExchangeResult success(int pointsGained,
                                                   int totalPoints,
                                                   Map<String, Integer> consumedItems) {
            Map<String, Integer> copy = new LinkedHashMap<>(Objects.requireNonNull(consumedItems, "consumedItems"));
            String message = "+" + pointsGained + " pts (현재 " + totalPoints + ")";
            return new ManualExchangeResult(true, pointsGained, totalPoints, copy, message);
        }

        public boolean wasProcessed() {
            return processed;
        }

        public int getPointsGained() {
            return pointsGained;
        }

        public int getTotalPoints() {
            return totalPoints;
        }

        public Map<String, Integer> getConsumedItems() {
            return consumedItems;
        }

        public String getNotificationMessage() {
            return notificationMessage;
        }
    }
}
