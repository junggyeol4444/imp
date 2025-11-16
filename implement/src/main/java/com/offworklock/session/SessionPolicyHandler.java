package com.offworklock.session;

import com.offworklock.config.ConfigData;
import com.offworklock.player.PlayerState;
import com.offworklock.player.PlayerStateManager;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Applies the session reset policy (one-time vs permanent unlock)
 * and tracks forced exit behaviour based on the last known session state.
 *
 * Integration layer responsibilities:
 * - Call {@link #handleSessionStart(String, UUID)} when a player joins.
 * - Call {@link #handleGracefulExit(String, UUID)} on a clean / intended exit path.
 */
public final class SessionPolicyHandler {

    private final PlayerStateManager playerStateManager;
    private final Supplier<ConfigData> configSupplier;
    private final AbuseTracker abuseTracker; // optional

    public SessionPolicyHandler(PlayerStateManager playerStateManager,
                                Supplier<ConfigData> configSupplier,
                                AbuseTracker abuseTracker) {
        this.playerStateManager = Objects.requireNonNull(playerStateManager, "playerStateManager");
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
        this.abuseTracker = abuseTracker;
    }

    /**
     * Handles a new session start for a player.
     * <ul>
     *   <li>If the previous state had {@code sessionOpen == true}, this is considered a forced exit.</li>
     *   <li>If forced exit tracking is enabled, the counter is incremented and hooks are notified.</li>
     *   <li>Session is marked as open for the new connection.</li>
     * </ul>
     */
    public SessionStartResult handleSessionStart(String contextId, UUID playerId) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");

        ConfigData config = Objects.requireNonNull(configSupplier.get(), "config");
        PlayerState state = playerStateManager.getOrCreateState(contextId, playerId);

        boolean forcedExitDetected = state.isSessionOpen();
        PlayerState updated = state.withSessionOpen(true);

        // Reset counter entirely when tracking is disabled.
        if (!config.isForcedExitTrackingEnabled() && updated.getForcedExitCount() != 0) {
            updated = updated.withForcedExitCount(0);
        }

        AbuseTracker.AbuseReport report = AbuseTracker.AbuseReport.none();

        if (forcedExitDetected && config.isForcedExitTrackingEnabled()) {
            int newCount = safeIncrement(state.getForcedExitCount());
            updated = updated.withForcedExitCount(newCount);

            if (abuseTracker != null) {
                report = abuseTracker.onForcedExit(contextId, playerId, updated, config);
                if (report == null) {
                    report = AbuseTracker.AbuseReport.none();
                }
            }
        }

        playerStateManager.updateState(contextId, playerId, updated);
        return new SessionStartResult(updated, forcedExitDetected, report);
    }

    /**
     * Handles a clean, intentional exit:
     * <ul>
     *   <li>{@code sessionOpen} is set to false.</li>
     *   <li>If ONE_TIME_UNLOCK is enabled and the player had unlocked off work, it is reset.</li>
     *   <li>If forced exit tracking is disabled, the counter is cleared.</li>
     * </ul>
     */
    public void handleGracefulExit(String contextId, UUID playerId) throws IOException {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");

        ConfigData config = Objects.requireNonNull(configSupplier.get(), "config");
        PlayerState state = playerStateManager.getOrCreateState(contextId, playerId);

        PlayerState updated = state.withSessionOpen(false);

        if (config.getSessionResetMode() == ConfigData.SessionResetMode.ONE_TIME_UNLOCK && state.canOffWork()) {
            updated = updated.withOffWork(false);
        }

        if (!config.isForcedExitTrackingEnabled() && updated.getForcedExitCount() != 0) {
            updated = updated.withForcedExitCount(0);
        }

        playerStateManager.updateState(contextId, playerId, updated);
    }

    private int safeIncrement(int value) {
        return (value == Integer.MAX_VALUE) ? Integer.MAX_VALUE : value + 1;
    }

    /**
     * Result of processing a session start.
     */
    public static final class SessionStartResult {
        private final PlayerState state;
        private final boolean forcedExitDetected;
        private final AbuseTracker.AbuseReport abuseReport;

        private SessionStartResult(PlayerState state,
                                   boolean forcedExitDetected,
                                   AbuseTracker.AbuseReport abuseReport) {
            this.state = Objects.requireNonNull(state, "state");
            this.forcedExitDetected = forcedExitDetected;
            this.abuseReport = (abuseReport == null)
                    ? AbuseTracker.AbuseReport.none()
                    : abuseReport;
        }

        /**
         * Updated player state after applying session rules.
         */
        public PlayerState getState() {
            return state;
        }

        /**
         * True if the previous session did not close cleanly
         * (i.e., {@code sessionOpen} was still true on join).
         */
        public boolean isForcedExitDetected() {
            return forcedExitDetected;
        }

        /**
         * Additional metadata from forced-exit tracking; never null.
         */
        public AbuseTracker.AbuseReport getAbuseReport() {
            return abuseReport;
        }
    }
}
