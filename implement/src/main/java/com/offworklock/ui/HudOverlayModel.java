package com.offworklock.ui;

import com.offworklock.config.ConfigData;
import com.offworklock.config.ConfigManager;
import com.offworklock.player.PlayerState;
import com.offworklock.player.PlayerStateManager;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Provides HUD overlay data showing the current point status and unlock state.
 * <p>
 * The integration layer should:
 * - Call {@link #build(String, UUID)} each render tick (or on a suitable interval),
 * - Read the returned {@link HudSnapshot},
 * - Render only when {@link HudSnapshot#isVisible()} is true.
 */
public final class HudOverlayModel {

    private final ConfigManager configManager;
    private final PlayerStateManager playerStateManager;

    public HudOverlayModel(ConfigManager configManager, PlayerStateManager playerStateManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.playerStateManager = Objects.requireNonNull(playerStateManager, "playerStateManager");
    }

    /**
     * Builds a snapshot of the HUD state for the given player.
     *
     * @param contextId server/world/context identifier
     * @param playerId  player UUID
     * @return immutable HUD snapshot; use {@link HudSnapshot#isVisible()} to decide rendering
     */
    public HudSnapshot build(String contextId, UUID playerId) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");

        ConfigData config = Objects.requireNonNull(configManager.getConfig(), "config");
        if (!config.isHudEnabled()) {
            return HudSnapshot.hidden();
        }

        PlayerState state = playerStateManager.getOrCreateState(contextId, playerId);
        int points = Math.max(0, state.getPoints());
        int gachaCost = Math.max(0, config.getGachaCost());

        return HudSnapshot.visible(
                points,
                gachaCost,
                state.canOffWork(),
                config.getHudOffsetX(),
                config.getHudOffsetY(),
                config.isHudShowCost(),
                config.isHudShowUnlockState()
        );
    }

    /**
     * Immutable HUD view model.
     */
    public static final class HudSnapshot {
        private final boolean visible;
        private final int points;
        private final int gachaCost;
        private final boolean canOffWork;
        private final int offsetX;
        private final int offsetY;
        private final boolean showCost;
        private final boolean showUnlockState;

        private HudSnapshot(boolean visible,
                            int points,
                            int gachaCost,
                            boolean canOffWork,
                            int offsetX,
                            int offsetY,
                            boolean showCost,
                            boolean showUnlockState) {
            this.visible = visible;
            this.points = points;
            this.gachaCost = gachaCost;
            this.canOffWork = canOffWork;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.showCost = showCost;
            this.showUnlockState = showUnlockState;
        }

        static HudSnapshot hidden() {
            return new HudSnapshot(false, 0, 0, false, 0, 0, false, false);
        }

        static HudSnapshot visible(int points,
                                   int gachaCost,
                                   boolean canOffWork,
                                   int offsetX,
                                   int offsetY,
                                   boolean showCost,
                                   boolean showUnlockState) {
            return new HudSnapshot(true, points, gachaCost, canOffWork, offsetX, offsetY, showCost, showUnlockState);
        }

        public boolean isVisible() {
            return visible;
        }

        public int getPoints() {
            return points;
        }

        public int getGachaCost() {
            return gachaCost;
        }

        public boolean canOffWork() {
            return canOffWork;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public boolean isShowCost() {
            return showCost;
        }

        public boolean isShowUnlockState() {
            return showUnlockState;
        }
    }
}
