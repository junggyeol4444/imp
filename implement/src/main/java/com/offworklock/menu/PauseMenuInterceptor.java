package com.offworklock.menu;

import com.offworklock.config.ConfigData;
import com.offworklock.dimension.DimensionLockService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Applies the pause menu locking rules:
 * - Target dimensions are configured via ConfigData.lockedDimensions.
 * - If the player has not unlocked "off work", exit-related buttons are disabled or hidden.
 * - If unlocked, the pause menu is left untouched.
 *
 * The Minecraft integration layer should:
 * 1) Build a {@link Request} from the current pause screen state.
 * 2) Call {@link #intercept(Request)}.
 * 3) Apply the returned {@link Result} when rendering the menu.
 */
public final class PauseMenuInterceptor {

    private static final Set<String> EXIT_ID_HINTS = Set.of(
            "menu.return_to_menu",
            "menu.returntomenu",
            "menu.returntomenu.button",
            "menu.disconnect",
            "menu.quit",
            "menu.quitworld",
            "menu.logout",
            "menu.exit",
            "button.disconnect",
            "button.quit",
            "button.exit"
    );

    private static final Set<String> EXIT_KEYWORDS = Set.of(
            "quit",
            "exit",
            "disconnect",
            "main menu",
            "title screen",
            "leave",
            "log out",
            "logout",
            "저장",
            "나가기",
            "종료",
            "메인"
    );

    private final DimensionLockService dimensionLockService;
    private final Supplier<ConfigData> configSupplier;

    public PauseMenuInterceptor(DimensionLockService dimensionLockService,
                                Supplier<ConfigData> configSupplier) {
        this.dimensionLockService = Objects.requireNonNull(dimensionLockService, "dimensionLockService");
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
    }

    /**
     * Applies lock rules to the given pause menu snapshot.
     *
     * @param request current pause menu state
     * @return new state with exit controls possibly disabled/hidden and an optional lock message
     */
    public Result intercept(Request request) {
        Objects.requireNonNull(request, "request");
        ConfigData config = Objects.requireNonNull(configSupplier.get(), "config");

        boolean shouldLock =
                request.isPlayerPresent()
                        && dimensionLockService.isLocked(request.getDimensionId())
                        && !request.canOffWork();

        // No locking: return as-is (buttons are wrapped in Result).
        if (!shouldLock) {
            return new Result(false, request.getButtons(), null);
        }

        List<ButtonState> originalButtons = request.getButtons();
        List<ButtonState> adjusted = new ArrayList<>(originalButtons.size());

        boolean hideButtons = config.isLockHideButtons();
        for (ButtonState button : originalButtons) {
            if (isExitButton(button)) {
                ButtonState updated = button.withActive(false);
                if (hideButtons) {
                    updated = updated.withVisible(false);
                }
                adjusted.add(updated);
            } else {
                adjusted.add(button);
            }
        }

        String configured = config.getLockMessage();
        String message = (configured == null || configured.isBlank())
                ? "퇴근 뽑기에서 퇴근에 당첨되어야 나갈 수 있습니다."
                : configured;

        return new Result(true, adjusted, message);
    }

    private boolean isExitButton(ButtonState button) {
        if (button == null) {
            return false;
        }
        if (button.isExitButtonHint()) {
            return true;
        }
        if (matchesHint(button.getIdentifier())) {
            return true;
        }
        if (matchesHint(button.getTranslationKey())) {
            return true;
        }
        return containsKeyword(button.getLabel());
    }

    private boolean matchesHint(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String lower = value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return EXIT_ID_HINTS.contains(lower);
    }

    private boolean containsKeyword(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String keyword : EXIT_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Input payload describing the current pause menu state.
     */
    public static final class Request {
        private final boolean playerPresent;
        private final String dimensionId;
        private final boolean canOffWork;
        private final List<ButtonState> buttons;

        public Request(boolean playerPresent,
                       String dimensionId,
                       boolean canOffWork,
                       List<ButtonState> buttons) {
            this.playerPresent = playerPresent;
            this.dimensionId = dimensionId;
            this.canOffWork = canOffWork;
            List<ButtonState> safeButtons = (buttons == null)
                    ? new ArrayList<>()
                    : new ArrayList<>(buttons);
            this.buttons = Collections.unmodifiableList(safeButtons);
        }

        public boolean isPlayerPresent() {
            return playerPresent;
        }

        public String getDimensionId() {
            return dimensionId;
        }

        public boolean canOffWork() {
            return canOffWork;
        }

        public List<ButtonState> getButtons() {
            return buttons;
        }
    }

    /**
     * Immutable description of a pause menu button.
     */
    public static final class ButtonState {
        private final String identifier;
        private final String translationKey;
        private final String label;
        private final boolean exitButtonHint;
        private final boolean active;
        private final boolean visible;

        public ButtonState(String identifier,
                           String translationKey,
                           String label,
                           boolean exitButtonHint,
                           boolean active,
                           boolean visible) {
            this.identifier = identifier;
            this.translationKey = translationKey;
            this.label = label;
            this.exitButtonHint = exitButtonHint;
            this.active = active;
            this.visible = visible;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public String getLabel() {
            return label;
        }

        public boolean isExitButtonHint() {
            return exitButtonHint;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isVisible() {
            return visible;
        }

        public ButtonState withActive(boolean active) {
            if (this.active == active) {
                return this;
            }
            return new ButtonState(identifier, translationKey, label, exitButtonHint, active, visible);
        }

        public ButtonState withVisible(boolean visible) {
            if (this.visible == visible) {
                return this;
            }
            return new ButtonState(identifier, translationKey, label, exitButtonHint, active, visible);
        }
    }

    /**
     * Resulting pause menu state after applying lock rules.
     */
    public static final class Result {
        private final boolean exitLocked;
        private final List<ButtonState> buttons;
        private final String lockMessage;

        private Result(boolean exitLocked, List<ButtonState> buttons, String lockMessage) {
            this.exitLocked = exitLocked;
            this.buttons = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(buttons, "buttons")));
            this.lockMessage = lockMessage;
        }

        /**
         * Whether exit-related controls are currently locked for this menu.
         */
        public boolean isExitLocked() {
            return exitLocked;
        }

        /**
         * Final button states that should be rendered.
         */
        public List<ButtonState> getButtons() {
            return buttons;
        }

        /**
         * Optional message explaining why exit is locked (may be null).
         */
        public String getLockMessage() {
            return lockMessage;
        }
    }
}
