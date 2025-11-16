package com.offworklock.ui;

import com.offworklock.config.ConfigData;
import com.offworklock.config.ConfigManager;
import com.offworklock.gacha.GachaService;
import com.offworklock.gacha.GachaService.RewardDisplay;
import com.offworklock.player.PlayerState;
import com.offworklock.player.PlayerStateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregates data required to render the gacha screen:
 * - Current player points
 * - Single-roll cost
 * - Whether the player can currently roll
 * - Reward table with normalized probabilities
 *
 * The integration layer should:
 * - Call {@link #snapshot(String, UUID)} when opening / refreshing the gacha UI,
 * - Use {@link ScreenSnapshot} as the sole data source for rendering.
 */
public final class GachaScreenModel {

    private final ConfigManager configManager;
    private final PlayerStateManager playerStateManager;
    private final GachaService gachaService;

    public GachaScreenModel(ConfigManager configManager,
                            PlayerStateManager playerStateManager,
                            GachaService gachaService) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.playerStateManager = Objects.requireNonNull(playerStateManager, "playerStateManager");
        this.gachaService = Objects.requireNonNull(gachaService, "gachaService");
    }

    /**
     * Builds a snapshot of all information needed to render the gacha screen for a player.
     *
     * @param contextId server/world/context identifier
     * @param playerId  player UUID
     * @return immutable screen snapshot
     */
    public ScreenSnapshot snapshot(String contextId, UUID playerId) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");

        ConfigData config = Objects.requireNonNull(configManager.getConfig(), "config");
        PlayerState state = playerStateManager.getOrCreateState(contextId, playerId);

        int points = Math.max(0, state.getPoints());
        int cost = Math.max(0, config.getGachaCost());

        List<RewardDisplay> rewardDisplays = gachaService.getRewardDisplays();
        List<RewardEntry> entries = new ArrayList<>(rewardDisplays.size());
        for (RewardDisplay display : rewardDisplays) {
            if (display == null || display.getReward() == null) {
                continue;
            }
            var reward = display.getReward();
            entries.add(new RewardEntry(
                    reward.getId(),
                    reward.getName(),
                    reward.getDescription(),
                    display.getProbability(),
                    display.getProbabilityText()
            ));
        }

        boolean canRoll = cost > 0 && points >= cost;

        return new ScreenSnapshot(points, cost, canRoll, entries);
    }

    /**
     * Immutable view model for the gacha screen.
     */
    public static final class ScreenSnapshot {
        private final int points;
        private final int cost;
        private final boolean canRoll;
        private final List<RewardEntry> rewards;

        private ScreenSnapshot(int points, int cost, boolean canRoll, List<RewardEntry> rewards) {
            this.points = points;
            this.cost = cost;
            this.canRoll = canRoll;
            this.rewards = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(rewards, "rewards")));
        }

        public int getPoints() {
            return points;
        }

        public int getCost() {
            return cost;
        }

        public boolean canRoll() {
            return canRoll;
        }

        public List<RewardEntry> getRewards() {
            return rewards;
        }
    }

    /**
     * UI-facing reward entry with metadata required for display.
     */
    public static final class RewardEntry {
        private final String id;
        private final String name;
        private final String description;
        private final double probability;
        private final String probabilityText;

        private RewardEntry(String id,
                            String name,
                            String description,
                            double probability,
                            String probabilityText) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.probability = probability;
            this.probabilityText = probabilityText;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getProbability() {
            return probability;
        }

        public String getProbabilityText() {
            return probabilityText;
        }
    }
}
