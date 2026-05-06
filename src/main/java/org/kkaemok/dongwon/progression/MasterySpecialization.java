package org.kkaemok.dongwon.progression;

import java.util.Locale;

public enum MasterySpecialization {
    NONE("none", "없음"),
    SHIHYEONRYU("shihyeonryu", "시현류"),
    JIN_SWORDSMAN("jin_swordsman", "진-검사");

    private final String key;
    private final String displayName;

    MasterySpecialization(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MasterySpecialization fromKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        String normalized = raw.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        for (MasterySpecialization value : values()) {
            if (value.key.equals(normalized)) {
                return value;
            }
        }
        return NONE;
    }

    public static MasterySpecialization fromInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        String normalized = raw.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");

        if (normalized.equals("시현류") || normalized.equals("sihyeonryu") || normalized.equals("shihyeonryu")) {
            return SHIHYEONRYU;
        }
        if (normalized.equals("진검사") || normalized.equals("jinswordsman") || normalized.equals("jin")) {
            return JIN_SWORDSMAN;
        }
        return NONE;
    }
}
