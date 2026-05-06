package org.kkaemok.dongwon.job;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum JobType {
    NONE("none", "없음", JobTier.COMMON),
    MINER("miner", "광부", JobTier.COMMON, "mining"),
    WARRIOR("warrior", "전사", JobTier.COMMON),
    SWORDSMAN("swordsman", "검사", JobTier.COMMON, "sword"),
    FISHERMAN("fisherman", "낚시꾼", JobTier.COMMON, "fishing"),
    GUMIHO("gumiho", "구미호", JobTier.UNCOMMON, "kumiho"),
    ORC("orc", "오크", JobTier.UNCOMMON),
    SUN_PRIEST("sun_priest", "태양의 사제", JobTier.RARE, "sunpriest"),
    ANTI_DEBUFFER("anti_debuffer", "안티디버퍼", JobTier.EPIC, "antidebuffer"),
    CHEONHO("cheonho", "천호", JobTier.LEGENDARY),
    SHINNONG("shinnong", "염제-신농", JobTier.LEGENDARY, "염제신농", "flame_emperor");

    private static final Map<String, JobType> LOOKUP = new HashMap<>();

    static {
        for (JobType value : values()) {
            register(value, value.name());
            register(value, value.key);
            register(value, value.displayName);
            value.aliases.forEach(alias -> register(value, alias));
        }
    }

    private final String key;
    private final String displayName;
    private final JobTier tier;
    private final Set<String> aliases;

    JobType(String key, String displayName, JobTier tier, String... aliases) {
        this.key = key;
        this.displayName = displayName;
        this.tier = tier;
        this.aliases = Arrays.stream(aliases).collect(Collectors.toUnmodifiableSet());
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public JobTier getTier() {
        return tier;
    }

    public boolean isAssignable() {
        return this != NONE;
    }

    public static @NotNull Optional<JobType> fromInput(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LOOKUP.get(normalize(input)));
    }

    public static @NotNull String assignableNames() {
        return Arrays.stream(values())
                .filter(JobType::isAssignable)
                .map(JobType::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    private static void register(JobType type, String key) {
        LOOKUP.put(normalize(key), type);
    }

    private static String normalize(String raw) {
        return raw
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}
