package com.offworklock;

import com.offworklock.config.ConfigData;
import com.offworklock.config.ConfigManager;
import com.offworklock.dimension.DimensionLockService;
import com.offworklock.gacha.GachaService;
import com.offworklock.menu.PauseMenuInterceptor;
import com.offworklock.player.PlayerStateManager;
import com.offworklock.points.PointService;
import com.offworklock.session.AbuseTracker;
import com.offworklock.session.SessionPolicyHandler;
import com.offworklock.ui.GachaScreenModel;
import com.offworklock.ui.HudOverlayModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Central bootstrap / facade for the Off Work Lock core.
 * <p>
 * This class wires together configuration, player state storage, gacha logic,
 * HUD models, pause-menu interceptor, and session policy handling.
 * <p>
 * A platform-specific integration (Fabric/Forge/Spigot/etc) should:
 * <ul>
 *     <li>Call {@link #init(Path, Path)} once during startup.</li>
 *     <li>Use the exposed getters to hook into events (join/quit, block break, GUI, etc.).</li>
 * </ul>
 * This class is intentionally free of any Minecraft API dependencies.
 */
public final class OffWorkLockMod {

    private static volatile OffWorkLockMod INSTANCE;

    private final ConfigManager configManager;
    private final PlayerStateManager playerStateManager;

    private final DimensionLockService dimensionLockService;
    private final PointService pointService;
    private final GachaService gachaService;
    private final HudOverlayModel hudOverlayModel;
    private final GachaScreenModel gachaScreenModel;
    private final AbuseTracker abuseTracker;
    private final SessionPolicyHandler sessionPolicyHandler;
    private final PauseMenuInterceptor pauseMenuInterceptor;

    private OffWorkLockMod(Path configDirectory, Path playerDataDirectory) throws IOException {
        Objects.requireNonNull(configDirectory, "configDirectory");
        Objects.requireNonNull(playerDataDirectory, "playerDataDirectory");

        this.configManager = new ConfigManager(configDirectory);
        this.playerStateManager = new PlayerStateManager(playerDataDirectory);

        this.dimensionLockService = new DimensionLockService(configManager);
        this.pointService = new PointService(configManager, playerStateManager);
        this.gachaService = new GachaService(configManager, playerStateManager);
        this.hudOverlayModel = new HudOverlayModel(configManager, playerStateManager);
        this.gachaScreenModel = new GachaScreenModel(configManager, playerStateManager, gachaService);

        this.abuseTracker = new AbuseTracker();
        this.sessionPolicyHandler = new SessionPolicyHandler(
                playerStateManager,
                configManager::getConfig,
                abuseTracker
        );

        this.pauseMenuInterceptor = new PauseMenuInterceptor(
                dimensionLockService,
                configManager::getConfig
        );
    }

    /**
     * Initializes the core singleton.
     *
     * @param configDirectory     directory for offwork-lock.cfg
     * @param playerDataDirectory directory for player state persistence
     */
    public static OffWorkLockMod init(Path configDirectory, Path playerDataDirectory) throws IOException {
        if (INSTANCE == null) {
            synchronized (OffWorkLockMod.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OffWorkLockMod(configDirectory, playerDataDirectory);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Returns the initialized instance.
     *
     * @throws IllegalStateException if {@link #init(Path, Path)} has not been called yet
     */
    public static OffWorkLockMod get() {
        OffWorkLockMod instance = INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("OffWorkLockMod is not initialized. Call init(...) first.");
        }
        return instance;
    }

    // ---------- Public accessors for integration layers ----------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    public DimensionLockService getDimensionLockService() {
        return dimensionLockService;
    }

    public PointService getPointService() {
        return pointService;
    }

    public GachaService getGachaService() {
        return gachaService;
    }

    public HudOverlayModel getHudOverlayModel() {
        return hudOverlayModel;
    }

    public GachaScreenModel getGachaScreenModel() {
        return gachaScreenModel;
    }

    public AbuseTracker getAbuseTracker() {
        return abuseTracker;
    }

    public SessionPolicyHandler getSessionPolicyHandler() {
        return sessionPolicyHandler;
    }

    public PauseMenuInterceptor getPauseMenuInterceptor() {
        return pauseMenuInterceptor;
    }

    // ---------- Convenience hooks (optional, platform wires these to real events) ----------

    /**
     * Should be called by the platform when a player joins.
     */
    public SessionPolicyHandler.SessionStartResult onPlayerJoin(String contextId, UUID playerId) throws IOException {
        return sessionPolicyHandler.handleSessionStart(contextId, playerId);
    }

    /**
     * Should be called by the platform when a player exits cleanly
     * via an allowed path (e.g., unlocked quit button).
     */
    public void onPlayerGracefulExit(String contextId, UUID playerId) throws IOException {
        sessionPolicyHandler.handleGracefulExit(contextId, playerId);
    }

    /**
     * Reloads configuration from disk.
     */
    public void reloadConfig() throws IOException {
        configManager.reload();
    }
}
