package com.offworklock.gacha;

import com.offworklock.config.ConfigData;
import com.offworklock.config.ConfigManager;
import com.offworklock.config.GachaReward;
import com.offworklock.player.PlayerState;
import com.offworklock.player.PlayerStateManager;
import com.offworklock.util.StringParsers;
import com.offworklock.util.WeightedPicker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Core gacha logic: cost handling, random selection, and effect resolution.
 */
public final class GachaService {

    private final ConfigManager configManager;
    private final PlayerStateManager playerStateManager;
    private final WeightedPicker weightedPicker;

    public GachaService(ConfigManager configManager, PlayerStateManager playerStateManager) {
        this(configManager, playerStateManager, new WeightedPicker());
    }

    GachaService(ConfigManager configManager,
                 PlayerStateManager playerStateManager,
                 WeightedPicker weightedPicker) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.playerStateManager = Objects.requireNonNull(playerStateManager, "playerStateManager");
        this.weightedPicker = Objects.requireNonNull(weightedPicker, "weightedPicker");
    }

    /**
     * Returns whether the player currently has enough points to roll once.
     */
    public boolean canRoll(String contextId, UUID playerId) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");

        ConfigData config = configManager.getConfig();
        PlayerState state = playerStateManager.getOrCreateState(contextId, playerId);
        return state.getPoints() >= config.getGachaCost();
    }

    /**
     * Executes a single gacha roll:
     * - Validates cost and reward table
     * - Deducts cost
     * - Applies immediate effects (unlock exit / add points / message)
     * - Collects deferred effects (currency / items / potion / custom)
     */
    public RollResult roll(String contextId, UUID playerId) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");

        ConfigData config = configManager.getConfig();
        List<GachaReward> rewards = config.getRewards();
        if (rewards.isEmpty()) {
            return RollResult.failure("보상 테이블이 비어 있습니다.");
        }

        PlayerState originalState = playerStateManager.getOrCreateState(contextId, playerId);
        int cost = config.getGachaCost();
        if (originalState.getPoints() < cost) {
            return RollResult.failure("포인트가 부족합니다.");
        }

        GachaReward reward = weightedPicker.pick(rewards, GachaReward::getWeight);
        if (reward == null) {
            return RollResult.failure("유효한 보상을 선택할 수 없습니다.");
        }

        // Deduct cost first (never below 0)
        int pointsAfterCost = Math.max(0, originalState.getPoints() - cost);
        PlayerState workingState = originalState.withPoints(pointsAfterCost);

        boolean unlockedExit = workingState.canOffWork();
        int bonusPoints = 0;

        List<GachaEffect> deferredEffects = new ArrayList<>();
        List<String> notifications = new ArrayList<>();
        List<GachaEffect> executedEffects = new ArrayList<>();

        List<String> effects = reward.getEffects();
        if (effects != null && !effects.isEmpty()) {
            for (String effectCommand : effects) {
                if (effectCommand == null || effectCommand.isBlank()) {
                    continue;
                }

                GachaEffect effect = GachaEffect.parse(effectCommand);
                executedEffects.add(effect);

                switch (effect.getType()) {
                    case UNLOCK_EXIT -> {
                        if (!workingState.canOffWork()) {
                            workingState = workingState.withOffWork(true);
                        }
                        unlockedExit = true;
                        notifications.add("퇴근 성공! ESC 메뉴가 다시 활성화됩니다.");
                    }

                    case ADD_POINTS -> {
                        int delta = effect.getArguments().isEmpty()
                                ? 0
                                : StringParsers.parseInt(effect.getArguments().get(0), 0);
                        if (delta != 0) {
                            int before = workingState.getPoints();
                            long updated = (long) before + delta;
                            int clamped = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, updated));
                            workingState = workingState.withPoints(clamped);
                            int applied = clamped - before;
                            if (applied != 0) {
                                bonusPoints += applied;
                                notifications.add("보너스 포인트 " + formatSigned(applied));
                            }
                        }
                    }

                    case MESSAGE -> {
                        String message = effect.getArguments().isEmpty()
                                ? effect.getRawCommand()
                                : String.join(":", effect.getArguments());
                        if (message != null && !message.isBlank()) {
                            notifications.add(message);
                        }
                    }

                    case ADD_CURRENCY, GIVE_ITEM, GIVE_EFFECT, CUSTOM -> {
                        // These are intentionally deferred; integration layer is responsible for execution.
                        deferredEffects.add(effect);
                    }
                }
            }
        }

        // Persist updated state
        playerStateManager.updateState(contextId, playerId, workingState);

        // Always show reward name first if available
        if (reward.getName() != null && !reward.getName().isBlank()) {
            notifications.add(0, reward.getName());
        }

        return RollResult.success(
                reward,
                cost,
                originalState.getPoints(),
                workingState.getPoints(),
                bonusPoints,
                unlockedExit,
                deferredEffects,
                notifications,
                executedEffects
        );
    }

    /**
     * Returns immutable list of reward displays with normalized probabilities for UI.
     */
    public List<RewardDisplay> getRewardDisplays() {
        ConfigData config = configManager.getConfig();
        List<GachaReward> rewards = config.getRewards();

        double totalWeight = rewards.stream()
                .mapToDouble(GachaReward::getWeight)
                .filter(w -> w > 0D)
                .sum();

        List<RewardDisplay> displays = new ArrayList<>(rewards.size());
        for (GachaReward reward : rewards) {
            double weight = Math.max(0D, reward.getWeight());
            double chance = (totalWeight <= 0D || weight <= 0D) ? 0D : (weight / totalWeight);
            displays.add(new RewardDisplay(reward, chance));
        }
        return Collections.unmodifiableList(displays);
    }

    private String formatSigned(int value) {
        return (value >= 0 ? "+" : "") + value + " pts";
    }

    /**
     * UI-facing reward entry with chance metadata.
     */
    public static final class RewardDisplay {
        private final GachaReward reward;
        private final double probability;

        private RewardDisplay(GachaReward reward, double probability) {
            this.reward = Objects.requireNonNull(reward, "reward");
            this.probability = probability;
        }

        public GachaReward getReward() {
            return reward;
        }

        public double getProbability() {
            return probability;
        }

        public String getProbabilityText() {
            double percent = probability * 100D;
            if (percent <= 0D) {
                return "0%";
            }
            if (percent >= 1D) {
                return String.format(Locale.ROOT, "%.1f%%", percent);
            }
            return String.format(Locale.ROOT, "%.2f%%", percent);
        }
    }

    public static final class RollResult {
        private final boolean success;
        private final String failureReason;

        private final GachaReward reward;
        private final int cost;
        private final int pointsBefore;
        private final int pointsAfter;
        private final int bonusPoints;
        private final boolean unlockedExit;

        private final List<GachaEffect> deferredEffects;
        private final List<String> notifications;
        private final List<GachaEffect> executedEffects;

        private RollResult(boolean success,
                           String failureReason,
                           GachaReward reward,
                           int cost,
                           int pointsBefore,
                           int pointsAfter,
                           int bonusPoints,
                           boolean unlockedExit,
                           List<GachaEffect> deferredEffects,
                           List<String> notifications,
                           List<GachaEffect> executedEffects) {
            this.success = success;
            this.failureReason = failureReason;
            this.reward = reward;
            this.cost = cost;
            this.pointsBefore = pointsBefore;
            this.pointsAfter = pointsAfter;
            this.bonusPoints = bonusPoints;
            this.unlockedExit = unlockedExit;
            this.deferredEffects = Collections.unmodifiableList(new ArrayList<>(deferredEffects));
            this.notifications = Collections.unmodifiableList(new ArrayList<>(notifications));
            this.executedEffects = Collections.unmodifiableList(new ArrayList<>(executedEffects));
        }

        public static RollResult failure(String reason) {
            Objects.requireNonNull(reason, "reason");
            return new RollResult(
                    false,
                    reason,
                    null,
                    0,
                    0,
                    0,
                    0,
                    false,
                    List.of(),
                    List.of(reason),
                    List.of()
            );
        }

        public static RollResult success(GachaReward reward,
                                         int cost,
                                         int pointsBefore,
                                         int pointsAfter,
                                         int bonusPoints,
                                         boolean unlockedExit,
                                         List<GachaEffect> deferredEffects,
                                         List<String> notifications,
                                         List<GachaEffect> executedEffects) {
            return new RollResult(
                    true,
                    null,
                    Objects.requireNonNull(reward, "reward"),
                    cost,
                    pointsBefore,
                    pointsAfter,
                    bonusPoints,
                    unlockedExit,
                    deferredEffects,
                    notifications,
                    executedEffects
            );
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public GachaReward getReward() {
            return reward;
        }

        public int getCost() {
            return cost;
        }

        public int getPointsBefore() {
            return pointsBefore;
        }

        public int getPointsAfter() {
            return pointsAfter;
        }

        public int getBonusPoints() {
            return bonusPoints;
        }

        public boolean isUnlockedExit() {
            return unlockedExit;
        }

        /**
         * Effects that must be applied by the platform / integration layer
         * (economy, items, potion effects, custom actions, etc).
         */
        public List<GachaEffect> getDeferredEffects() {
            return deferredEffects;
        }

        /**
         * Ordered messages intended for UI/Chat.
         */
        public List<String> getNotifications() {
            return notifications;
        }

        /**
         * All parsed effects for this roll (including deferred ones),
         * useful for debugging or analytics.
         */
        public List<GachaEffect> getExecutedEffects() {
            return executedEffects;
        }
    }
}
