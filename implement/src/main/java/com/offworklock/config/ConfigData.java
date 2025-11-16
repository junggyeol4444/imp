package com.offworklock.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration data loaded from the user facing configuration file.
 */
public final class ConfigData {

    private final List<String> lockedDimensions;
    private final Map<String, Integer> oreValues;
    private final int gachaCost;
    private final List<GachaReward> rewards;
    private final PointAccumulationMode pointAccumulationMode;
    private final SessionResetMode sessionResetMode;
    private final boolean hudEnabled;
    private final int hudOffsetX;
    private final int hudOffsetY;
    private final boolean hudShowCost;
    private final boolean hudShowUnlockState;
    private final boolean lockHideButtons;
    private final String lockMessage;
    private final boolean forcedExitTrackingEnabled;
    private final int forcedExitWarningThreshold;
    private final String forcedExitWarningMessage;

    private ConfigData(Builder builder) {
        this.lockedDimensions = Collections.unmodifiableList(new ArrayList<>(builder.lockedDimensions));
        this.oreValues = Collections.unmodifiableMap(new LinkedHashMap<>(builder.oreValues));
        this.gachaCost = builder.gachaCost;
        this.rewards = Collections.unmodifiableList(new ArrayList<>(builder.rewards));
        this.pointAccumulationMode = builder.pointAccumulationMode;
        this.sessionResetMode = builder.sessionResetMode;
        this.hudEnabled = builder.hudEnabled;
        this.hudOffsetX = builder.hudOffsetX;
        this.hudOffsetY = builder.hudOffsetY;
        this.hudShowCost = builder.hudShowCost;
        this.hudShowUnlockState = builder.hudShowUnlockState;
        this.lockHideButtons = builder.lockHideButtons;
        this.lockMessage = builder.lockMessage;
        this.forcedExitTrackingEnabled = builder.forcedExitTrackingEnabled;
        this.forcedExitWarningThreshold = builder.forcedExitWarningThreshold;
        this.forcedExitWarningMessage = builder.forcedExitWarningMessage;
    }

    public List<String> getLockedDimensions() {
        return lockedDimensions;
    }

    public Map<String, Integer> getOreValues() {
        return oreValues;
    }

    public int getGachaCost() {
        return gachaCost;
    }

    public List<GachaReward> getRewards() {
        return rewards;
    }

    public PointAccumulationMode getPointAccumulationMode() {
        return pointAccumulationMode;
    }

    public SessionResetMode getSessionResetMode() {
        return sessionResetMode;
    }

    public boolean isHudEnabled() {
        return hudEnabled;
    }

    public int getHudOffsetX() {
        return hudOffsetX;
    }

    public int getHudOffsetY() {
        return hudOffsetY;
    }

    public boolean isHudShowCost() {
        return hudShowCost;
    }

    public boolean isHudShowUnlockState() {
        return hudShowUnlockState;
    }

    public boolean isLockHideButtons() {
        return lockHideButtons;
    }

    public String getLockMessage() {
        return lockMessage;
    }

    public boolean isForcedExitTrackingEnabled() {
        return forcedExitTrackingEnabled;
    }

    public int getForcedExitWarningThreshold() {
        return forcedExitWarningThreshold;
    }

    public String getForcedExitWarningMessage() {
        return forcedExitWarningMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public enum PointAccumulationMode {
        AUTOMATIC,
        MANUAL
    }

    public enum SessionResetMode {
        ONE_TIME_UNLOCK,
        PERMANENT_UNLOCK
    }

    public static final class Builder {
        private List<String> lockedDimensions = new ArrayList<>();
        private Map<String, Integer> oreValues = new LinkedHashMap<>();
        private int gachaCost = 0;
        private List<GachaReward> rewards = new ArrayList<>();
        private PointAccumulationMode pointAccumulationMode = PointAccumulationMode.AUTOMATIC;
        private SessionResetMode sessionResetMode = SessionResetMode.ONE_TIME_UNLOCK;
        private boolean hudEnabled = true;
        private int hudOffsetX = 4;
        private int hudOffsetY = 4;
        private boolean hudShowCost = true;
        private boolean hudShowUnlockState = true;
        private boolean lockHideButtons = false;
        private String lockMessage = "퇴근 뽑기에서 퇴근을 뽑아야 나갈 수 있습니다.";
        private boolean forcedExitTrackingEnabled = true;
        private int forcedExitWarningThreshold = 3;
        private String forcedExitWarningMessage = "강제 종료 시도가 {count}회 감지되었습니다.";

        private Builder() {
        }

        private Builder(ConfigData source) {
            this.lockedDimensions = new ArrayList<>(source.lockedDimensions);
            this.oreValues = new LinkedHashMap<>(source.oreValues);
            this.gachaCost = source.gachaCost;
            this.rewards = new ArrayList<>(source.rewards);
            this.pointAccumulationMode = source.pointAccumulationMode;
            this.sessionResetMode = source.sessionResetMode;
            this.hudEnabled = source.hudEnabled;
            this.hudOffsetX = source.hudOffsetX;
            this.hudOffsetY = source.hudOffsetY;
            this.hudShowCost = source.hudShowCost;
            this.hudShowUnlockState = source.hudShowUnlockState;
            this.lockHideButtons = source.lockHideButtons;
            this.lockMessage = source.lockMessage;
            this.forcedExitTrackingEnabled = source.forcedExitTrackingEnabled;
            this.forcedExitWarningThreshold = source.forcedExitWarningThreshold;
            this.forcedExitWarningMessage = source.forcedExitWarningMessage;
        }

        public Builder lockedDimensions(List<String> lockedDimensions) {
            this.lockedDimensions = new ArrayList<>(Objects.requireNonNull(lockedDimensions, "lockedDimensions"));
            return this;
        }

        public Builder addLockedDimension(String dimensionId) {
            this.lockedDimensions.add(Objects.requireNonNull(dimensionId, "dimensionId"));
            return this;
        }

        public Builder oreValues(Map<String, Integer> oreValues) {
            this.oreValues = new LinkedHashMap<>(Objects.requireNonNull(oreValues, "oreValues"));
            return this;
        }

        public Builder putOreValue(String blockId, int value) {
            this.oreValues.put(Objects.requireNonNull(blockId, "blockId"), value);
            return this;
        }

        public Builder gachaCost(int gachaCost) {
            this.gachaCost = gachaCost;
            return this;
        }

        public Builder rewards(List<GachaReward> rewards) {
            this.rewards = new ArrayList<>(Objects.requireNonNull(rewards, "rewards"));
            return this;
        }

        public Builder addReward(GachaReward reward) {
            this.rewards.add(Objects.requireNonNull(reward, "reward"));
            return this;
        }

        public Builder pointAccumulationMode(PointAccumulationMode mode) {
            this.pointAccumulationMode = Objects.requireNonNull(mode, "pointAccumulationMode");
            return this;
        }

        public Builder sessionResetMode(SessionResetMode mode) {
            this.sessionResetMode = Objects.requireNonNull(mode, "sessionResetMode");
            return this;
        }

        public Builder hudEnabled(boolean hudEnabled) {
            this.hudEnabled = hudEnabled;
            return this;
        }

        public Builder hudOffsetX(int hudOffsetX) {
            this.hudOffsetX = hudOffsetX;
            return this;
        }

        public Builder hudOffsetY(int hudOffsetY) {
            this.hudOffsetY = hudOffsetY;
            return this;
        }

        public Builder hudShowCost(boolean hudShowCost) {
            this.hudShowCost = hudShowCost;
            return this;
        }

        public Builder hudShowUnlockState(boolean hudShowUnlockState) {
            this.hudShowUnlockState = hudShowUnlockState;
            return this;
        }

        public Builder lockHideButtons(boolean lockHideButtons) {
            this.lockHideButtons = lockHideButtons;
            return this;
        }

        public Builder lockMessage(String lockMessage) {
            this.lockMessage = Objects.requireNonNull(lockMessage, "lockMessage");
            return this;
        }

        public Builder forcedExitTrackingEnabled(boolean forcedExitTrackingEnabled) {
            this.forcedExitTrackingEnabled = forcedExitTrackingEnabled;
            return this;
        }

        public Builder forcedExitWarningThreshold(int forcedExitWarningThreshold) {
            this.forcedExitWarningThreshold = Math.max(0, forcedExitWarningThreshold);
            return this;
        }

        public Builder forcedExitWarningMessage(String forcedExitWarningMessage) {
            this.forcedExitWarningMessage = Objects.requireNonNull(forcedExitWarningMessage, "forcedExitWarningMessage");
            return this;
        }

        public ConfigData build() {
            return new ConfigData(this);
        }
    }
}
