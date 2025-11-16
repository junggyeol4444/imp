package com.offworklock.player;

import java.util.Objects;

/**
 * Stores the current point total and off-work unlock flag for a player.
 */
public final class PlayerState {
    private final int points;
    private final boolean canOffWork;
    private final boolean sessionOpen;
    private final int forcedExitCount;

    public PlayerState(int points, boolean canOffWork) {
        this(points, canOffWork, false, 0);
    }

    public PlayerState(int points, boolean canOffWork, boolean sessionOpen, int forcedExitCount) {
        this.points = points;
        this.canOffWork = canOffWork;
        this.sessionOpen = sessionOpen;
        this.forcedExitCount = Math.max(0, forcedExitCount);
    }

    public int getPoints() {
        return points;
    }

    public boolean canOffWork() {
        return canOffWork;
    }

    public PlayerState withPoints(int points) {
        return new PlayerState(points, this.canOffWork, this.sessionOpen, this.forcedExitCount);
    }

    public PlayerState withOffWork(boolean canOffWork) {
        return new PlayerState(this.points, canOffWork, this.sessionOpen, this.forcedExitCount);
    }

    public boolean isSessionOpen() {
        return sessionOpen;
    }

    public PlayerState withSessionOpen(boolean sessionOpen) {
        return new PlayerState(this.points, this.canOffWork, sessionOpen, this.forcedExitCount);
    }

    public int getForcedExitCount() {
        return forcedExitCount;
    }

    public PlayerState withForcedExitCount(int forcedExitCount) {
        return new PlayerState(this.points, this.canOffWork, this.sessionOpen, forcedExitCount);
    }

    @Override
    public String toString() {
        return "PlayerState{" +
                "points=" + points +
                ", canOffWork=" + canOffWork +
                ", sessionOpen=" + sessionOpen +
                ", forcedExitCount=" + forcedExitCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerState that = (PlayerState) o;
        return points == that.points
                && canOffWork == that.canOffWork
                && sessionOpen == that.sessionOpen
                && forcedExitCount == that.forcedExitCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(points, canOffWork, sessionOpen, forcedExitCount);
    }
}
