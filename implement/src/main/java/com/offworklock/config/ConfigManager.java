package com.offworklock.config;

import com.offworklock.util.StringParsers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Loads and stores the configuration used by the mod.
 * <p>
 * The configuration file is intentionally text based and easy to edit by hand. Each line uses a
 * {@code key=value} structure. Collection values are separated by commas and may contain spaces.
 */
public final class ConfigManager {

    private static final String COMMENT_PREFIX = "#";
    private static final String REWARD_FIELD_DELIMITER = "|";
    private static final String EFFECT_DELIMITER = ";";

    private final Path configFile;
    private volatile ConfigData cachedConfig;

    public ConfigManager(Path configDirectory) throws IOException {
        Objects.requireNonNull(configDirectory, "configDirectory");
        Files.createDirectories(configDirectory);
        this.configFile = configDirectory.resolve("offwork-lock.cfg");
        ensureConfigFile();
        this.cachedConfig = loadInternal();
    }

    /**
     * Returns the last loaded configuration snapshot.
     * Thread-safe for concurrent reads.
     */
    public ConfigData getConfig() {
        return cachedConfig;
    }

    /**
     * Reloads configuration from disk and updates the cached snapshot.
     */
    public void reload() throws IOException {
        this.cachedConfig = loadInternal();
    }

    /**
     * Persists the given configuration to disk and updates the cache.
     */
    public void save(ConfigData config) throws IOException {
        Objects.requireNonNull(config, "config");
        List<String> lines = serialize(config);
        Files.write(configFile, lines, StandardCharsets.UTF_8);
        this.cachedConfig = config;
    }

    private void ensureConfigFile() throws IOException {
        if (Files.exists(configFile)) {
            return;
        }
        ConfigData defaults = createDefaultConfig();
        List<String> lines = serialize(defaults);
        Files.write(configFile, lines, StandardCharsets.UTF_8);
    }

    private ConfigData loadInternal() throws IOException {
        List<String> lines = Files.readAllLines(configFile, StandardCharsets.UTF_8);
        Map<String, String> rawValues = new LinkedHashMap<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(COMMENT_PREFIX)) {
                continue;
            }
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex < 0) {
                continue;
            }
            String key = trimmed.substring(0, equalsIndex).trim();
            String value = trimmed.substring(equalsIndex + 1).trim();
            if (!key.isEmpty()) {
                rawValues.put(key, value);
            }
        }

        ConfigData.Builder builder = ConfigData.builder();

        builder.lockedDimensions(StringParsers.splitList(
                rawValues.getOrDefault(
                        "lockedDimensions",
                        "minecraft:overworld,minecraft:the_nether,minecraft:the_end"
                )));

        builder.oreValues(parseOreValues(rawValues.get("oreValues")));

        builder.gachaCost(StringParsers.parseInt(rawValues.get("gachaCost"), 100));

        builder.rewards(parseRewards(rawValues.get("rewards")));

        builder.pointAccumulationMode(StringParsers.parseEnum(
                ConfigData.PointAccumulationMode.class,
                rawValues.get("pointMode"),
                ConfigData.PointAccumulationMode.AUTOMATIC
        ));

        builder.sessionResetMode(StringParsers.parseEnum(
                ConfigData.SessionResetMode.class,
                rawValues.get("sessionMode"),
                ConfigData.SessionResetMode.ONE_TIME_UNLOCK
        ));

        builder.hudEnabled(StringParsers.parseBoolean(rawValues.get("hudEnabled"), true));
        builder.hudOffsetX(StringParsers.parseInt(rawValues.get("hudOffsetX"), 4));
        builder.hudOffsetY(StringParsers.parseInt(rawValues.get("hudOffsetY"), 4));
        builder.hudShowCost(StringParsers.parseBoolean(rawValues.get("hudShowCost"), true));
        builder.hudShowUnlockState(StringParsers.parseBoolean(rawValues.get("hudShowUnlockState"), true));

        builder.lockHideButtons(StringParsers.parseBoolean(rawValues.get("lockHideButtons"), false));
        builder.lockMessage(rawValues.getOrDefault(
                "lockMessage",
                "퇴근 뽑기에서 퇴근을 뽑아야 나갈 수 있습니다."
        ));

        builder.forcedExitTrackingEnabled(StringParsers.parseBoolean(
                rawValues.get("forcedExitTracking"),
                true
        ));
        builder.forcedExitWarningThreshold(StringParsers.parseInt(
                rawValues.get("forcedExitWarningThreshold"),
                3
        ));
        builder.forcedExitWarningMessage(rawValues.getOrDefault(
                "forcedExitWarningMessage",
                "강제 종료 시도가 {count}회 감지되었습니다."
        ));

        return builder.build();
    }

    private List<String> serialize(ConfigData config) {
        List<String> lines = new ArrayList<>();
        lines.add("# Off Work Lock configuration");
        lines.add("# lockedDimensions: comma separated list of dimension identifiers");
        lines.add("lockedDimensions=" + String.join(",", config.getLockedDimensions()));
        lines.add("# oreValues: comma separated entries of <blockId>:<points>");
        lines.add("oreValues=" + joinMap(config.getOreValues()));
        lines.add("# gachaCost: integer cost to roll once");
        lines.add("gachaCost=" + config.getGachaCost());
        lines.add("# rewards: comma separated entries of id|name|description|weight|effect1;effect2");
        lines.add("rewards=" + joinRewards(config.getRewards()));
        lines.add("# pointMode: AUTOMATIC or MANUAL");
        lines.add("pointMode=" + config.getPointAccumulationMode());
        lines.add("# sessionMode: ONE_TIME_UNLOCK or PERMANENT_UNLOCK");
        lines.add("sessionMode=" + config.getSessionResetMode());
        lines.add("# hudEnabled: whether to render the points HUD overlay");
        lines.add("hudEnabled=" + config.isHudEnabled());
        lines.add("# hudOffsetX/Y: pixel offset from the configured anchor (default top-left)");
        lines.add("hudOffsetX=" + config.getHudOffsetX());
        lines.add("hudOffsetY=" + config.getHudOffsetY());
        lines.add("# hudShowCost: include gacha cost in the HUD");
        lines.add("hudShowCost=" + config.isHudShowCost());
        lines.add("# hudShowUnlockState: include unlock status in the HUD");
        lines.add("hudShowUnlockState=" + config.isHudShowUnlockState());
        lines.add("# lockHideButtons: hide exit buttons entirely when locked (otherwise disable)");
        lines.add("lockHideButtons=" + config.isLockHideButtons());
        lines.add("# lockMessage: text shown on the pause menu while exit is locked");
        lines.add("lockMessage=" + config.getLockMessage());
        lines.add("# forcedExitTracking: enable forced exit counter and warnings");
        lines.add("forcedExitTracking=" + config.isForcedExitTrackingEnabled());
        lines.add("# forcedExitWarningThreshold: forced exit count before showing warnings (0 = disable)");
        lines.add("forcedExitWarningThreshold=" + config.getForcedExitWarningThreshold());
        lines.add("# forcedExitWarningMessage: message template for forced exit warnings ({count} placeholder)");
        lines.add("forcedExitWarningMessage=" + config.getForcedExitWarningMessage());
        return lines;
    }

    /**
     * Parses ore values from a string like:
     * minecraft:coal_ore:1,minecraft:iron_ore:2
     * using the last ':' in each entry as the separator between id and points.
     */
    private Map<String, Integer> parseOreValues(String rawValue) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (rawValue == null || rawValue.isEmpty()) {
            // Default values
            result.put("minecraft:coal_ore", 1);
            result.put("minecraft:iron_ore", 2);
            result.put("minecraft:gold_ore", 3);
            result.put("minecraft:diamond_ore", 5);
            return result;
        }

        for (String entry : rawValue.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int sep = trimmed.lastIndexOf(':');
            if (sep <= 0 || sep >= trimmed.length() - 1) {
                continue;
            }

            String blockId = trimmed.substring(0, sep).trim();
            String valuePart = trimmed.substring(sep + 1).trim();
            if (blockId.isEmpty()) {
                continue;
            }

            int value = StringParsers.parseInt(valuePart, 0);
            result.put(blockId, value);
        }

        return result;
    }

    private List<GachaReward> parseRewards(String rawValue) {
        List<GachaReward> rewards = new ArrayList<>();

        if (rawValue == null || rawValue.isEmpty()) {
            rewards.add(new GachaReward(
                    "OFF_WORK",
                    "퇴근 언락",
                    "세션 종료 제한을 해제합니다.",
                    1D,
                    List.of("unlock_exit")
            ));
            rewards.add(new GachaReward(
                    "POINTS_BONUS",
                    "보너스 포인트",
                    "추가 포인트를 획득합니다.",
                    3D,
                    List.of("add_points:50")
            ));
            rewards.add(new GachaReward(
                    "NOTHING",
                    "꽝",
                    "아무 일도 일어나지 않습니다.",
                    5D,
                    List.of("message:better-luck-next-time")
            ));
            return rewards;
        }

        for (String entry : rawValue.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split("\\" + REWARD_FIELD_DELIMITER, -1);
            if (parts.length < 4) {
                continue;
            }

            String id = parts[0].trim();
            if (id.isEmpty()) {
                continue;
            }

            String name = parts[1].trim();
            if (name.isEmpty()) {
                name = id;
            }

            String description = parts[2].trim();
            double weight = StringParsers.parseDouble(parts[3], 1D);

            List<String> effects = List.of();
            if (parts.length > 4) {
                effects = parseEffects(parts[4]);
            }

            rewards.add(new GachaReward(id, name, description, weight, effects));
        }

        return rewards;
    }

    private List<String> parseEffects(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        List<String> effects = new ArrayList<>();
        for (String part : value.split(EFFECT_DELIMITER)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                effects.add(trimmed);
            }
        }
        return effects;
    }

    private String joinMap(Map<String, Integer> map) {
        StringJoiner joiner = new StringJoiner(",");
        map.forEach((key, value) -> joiner.add(key + ":" + value));
        return joiner.toString();
    }

    private String joinRewards(List<GachaReward> rewards) {
        StringJoiner joiner = new StringJoiner(",");
        for (GachaReward reward : rewards) {
            String effectsJoined = reward.getEffects().isEmpty()
                    ? ""
                    : String.join(EFFECT_DELIMITER, reward.getEffects());

            String[] fields = new String[]{
                    reward.getId(),
                    reward.getName(),
                    reward.getDescription(),
                    String.valueOf(reward.getWeight()),
                    effectsJoined
            };

            int lastIndex = fields.length - 1;
            while (lastIndex >= 0 && fields[lastIndex].isEmpty()) {
                lastIndex--;
            }

            StringJoiner fieldJoiner = new StringJoiner(REWARD_FIELD_DELIMITER);
            for (int i = 0; i <= lastIndex; i++) {
                fieldJoiner.add(fields[i]);
            }

            joiner.add(fieldJoiner.toString());
        }
        return joiner.toString();
    }

    private ConfigData createDefaultConfig() {
        ConfigData.Builder builder = ConfigData.builder();

        builder.addLockedDimension("minecraft:overworld");
        builder.addLockedDimension("minecraft:the_nether");
        builder.addLockedDimension("minecraft:the_end");

        builder.putOreValue("minecraft:coal_ore", 1);
        builder.putOreValue("minecraft:iron_ore", 2);
        builder.putOreValue("minecraft:gold_ore", 3);
        builder.putOreValue("minecraft:diamond_ore", 5);

        builder.gachaCost(100);

        builder.addReward(new GachaReward(
                "OFF_WORK",
                "퇴근 언락",
                "세션 종료 제한을 해제합니다.",
                1D,
                List.of("unlock_exit")
        ));
        builder.addReward(new GachaReward(
                "POINTS_BONUS",
                "보너스 포인트",
                "추가 포인트를 획득합니다.",
                3D,
                List.of("add_points:50")
        ));
        builder.addReward(new GachaReward(
                "NOTHING",
                "꽝",
                "아무 일도 일어나지 않습니다.",
                5D,
                List.of("message:better-luck-next-time")
        ));

        builder.pointAccumulationMode(ConfigData.PointAccumulationMode.AUTOMATIC);
        builder.sessionResetMode(ConfigData.SessionResetMode.ONE_TIME_UNLOCK);

        builder.hudEnabled(true);
        builder.hudOffsetX(4);
        builder.hudOffsetY(4);
        builder.hudShowCost(true);
        builder.hudShowUnlockState(true);

        builder.lockHideButtons(false);
        builder.lockMessage("퇴근 뽑기에서 퇴근을 뽑아야 나갈 수 있습니다.");

        builder.forcedExitTrackingEnabled(true);
        builder.forcedExitWarningThreshold(3);
        builder.forcedExitWarningMessage("강제 종료 시도가 {count}회 감지되었습니다.");

        return builder.build();
    }
}
