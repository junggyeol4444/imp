package com.offworklock.session;

import com.offworklock.config.ConfigData;
import com.offworklock.player.PlayerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Optional component used to detect repeated forced exits and trigger custom penalties or
 * notifications. This class itself is side-effect free with respect to core logic; it only
 * notifies registered hooks and returns a summary report.
 */
public final class AbuseTracker {

    private final List<ForcedExitHook> hooks = new ArrayList<>();

    /**
     * Registers a new hook for forced-exit events.
     */
    public void addHook(ForcedExitHook hook) {
        if (hook != null) {
            hooks.add(hook);
        }
    }

    /**
     * Unregisters a previously added hook.
     */
    public void removeHook(ForcedExitHook hook) {
        if (hook != null) {
            hooks.remove(hook);
        }
    }

    /**
     * Returns an immutable snapshot of the currently registered hooks.
     */
    public List<ForcedExitHook> getHooks() {
        return Collections.unmodifiableList(new ArrayList<>(hooks));
    }

    /**
     * Invoked when a forced exit has been detected for a player.
     * <ul>
     *     <li>Evaluates whether the configured threshold has been reached.</li>
     *     <li>Builds an optional warning message with the current count.</li>
     *     <li>Notifies all registered hooks (exceptions are caught and ignored).</li>
     * </ul>
     *
     * @param contextId identifier for the current server/world/context (may be used by hooks)
     * @param playerId  player UUID (may be used by hooks)
     * @param state     latest persisted player state (including updated forcedExitCount)
     * @param config    configuration snapshot
     * @return {@link AbuseReport} describing whether the threshold was reached and the message to show
     */
    public AbuseReport onForcedExit(String contextId,
                                    UUID playerId,
                                    PlayerState state,
                                    ConfigData config) {
        Objects.requireNonNull(contextId, "contextId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(config, "config");

        boolean trackingEnabled = config.isForcedExitTrackingEnabled();
        int threshold = trackingEnabled
                ? Math.max(0, config.getForcedExitWarningThreshold())
                : 0;

        boolean thresholdReached = trackingEnabled
                && threshold > 0
                && state.getForcedExitCount() >= threshold;

        String warning = null;
        if (thresholdReached) {
            String template = Optional.ofNullable(config.getForcedExitWarningMessage())
                    .filter(message -> !message.isBlank())
                    .orElse("강제 종료 패턴이 감지되었습니다. ({count})");
            warning = template.replace("{count}", String.valueOf(state.getForcedExitCount()));
        }

        for (ForcedExitHook hook : hooks) {
            try {
                hook.onForcedExit(contextId, playerId, state, thresholdReached);
            } catch (RuntimeException ignored) {
                // Hooks must never break the core tracking flow.
            }
        }

        return new AbuseReport(thresholdReached, warning);
    }

    /**
     * Callback interface for consumers that want to react to forced exit patterns.
     */
    public interface ForcedExitHook {
        /**
         * Called when a forced exit is detected.
         *
         * @param contextId         server/world/context identifier
         * @param playerId          player UUID
         * @param state             latest player state
         * @param thresholdReached  whether the configured warning threshold has been reached or exceeded
         */
        void onForcedExit(String contextId, UUID playerId, PlayerState state, boolean thresholdReached);
    }

    /**
     * Lightweight value object describing the outcome of a forced-exit evaluation.
     */
    public static final class AbuseReport {
        private static final AbuseReport NONE = new AbuseReport(false, null);

        private final boolean thresholdReached;
        private final String warningMessage;

        private AbuseReport(boolean thresholdReached, String warningMessage) {
            this.thresholdReached = thresholdReached;
            this.warningMessage = warningMessage;
        }

        /**
         * Returns a shared instance representing "no issues".
         */
        public static AbuseReport none() {
            return NONE;
        }

        /**
         * True if the forced-exit count is at or above the configured threshold.
         */
        public boolean isThresholdReached() {
            return thresholdReached;
        }

        /**
         * Optional warning message to present to the player or logs (may be null).
         */
        public String getWarningMessage() {
            return warningMessage;
        }
    }
}
