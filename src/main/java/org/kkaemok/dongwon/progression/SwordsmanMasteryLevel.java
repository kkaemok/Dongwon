package org.kkaemok.dongwon.progression;

public enum SwordsmanMasteryLevel {
    LEVEL_0(0, 0L, 0L, 0L),
    LEVEL_1(1, 100L, 10_000L, 0L),
    LEVEL_2(2, 1_000L, 100_000L, 50_000L),
    LEVEL_3(3, 10_000L, 500_000L, 250_000L);

    private final int level;
    private final long requiredExp;
    private final long canReward;
    private final long silverCanReward;

    SwordsmanMasteryLevel(int level, long requiredExp, long canReward, long silverCanReward) {
        this.level = level;
        this.requiredExp = requiredExp;
        this.canReward = canReward;
        this.silverCanReward = silverCanReward;
    }

    public int getLevel() {
        return level;
    }

    public long getCanReward() {
        return canReward;
    }

    public long getSilverCanReward() {
        return silverCanReward;
    }

    public static SwordsmanMasteryLevel byLevel(int level) {
        for (SwordsmanMasteryLevel value : values()) {
            if (value.level == level) {
                return value;
            }
        }
        return LEVEL_0;
    }

    public static int resolveLevelByExp(long exp) {
        if (exp >= LEVEL_3.requiredExp) {
            return LEVEL_3.level;
        }
        if (exp >= LEVEL_2.requiredExp) {
            return LEVEL_2.level;
        }
        if (exp >= LEVEL_1.requiredExp) {
            return LEVEL_1.level;
        }
        return LEVEL_0.level;
    }
}
