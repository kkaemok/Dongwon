package org.kkaemok.dongwon.progression;

public enum FishermanMasteryLevel {
    LEVEL_0(0, "기본"),
    LEVEL_1(1_000, "1단계"),
    LEVEL_2(5_000, "2단계"),
    LEVEL_3(50_000, "3단계");

    private final long requiredExp;
    private final String displayName;

    FishermanMasteryLevel(long requiredExp, String displayName) {
        this.requiredExp = requiredExp;
        this.displayName = displayName;
    }

    public long getRequiredExp() {
        return requiredExp;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FishermanMasteryLevel next() {
        int nextOrdinal = ordinal() + 1;
        FishermanMasteryLevel[] values = values();
        if (nextOrdinal >= values.length) {
            return null;
        }
        return values[nextOrdinal];
    }

    public static FishermanMasteryLevel resolveByExp(long exp) {
        if (exp >= LEVEL_3.requiredExp) {
            return LEVEL_3;
        }
        if (exp >= LEVEL_2.requiredExp) {
            return LEVEL_2;
        }
        if (exp >= LEVEL_1.requiredExp) {
            return LEVEL_1;
        }
        return LEVEL_0;
    }
}
