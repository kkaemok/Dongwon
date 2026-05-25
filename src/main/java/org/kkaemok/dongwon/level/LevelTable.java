package org.kkaemok.dongwon.level;

public final class LevelTable {
    public static final int MAX_LEVEL = 50;
    private static final double[] REQUIRED_EXP = {
            0.0D,
            10.0D,
            20.0D,
            30.0D,
            40.0D,
            100.0D,
            120.0D,
            140.0D,
            160.0D,
            180.0D,
            500.0D,
            560.0D,
            620.0D,
            680.0D,
            740.0D,
            800.0D,
            1_500.0D,
            1_700.0D,
            1_900.0D,
            2_100.0D,
            2_500.0D,
            3_000.0D,
            3_500.0D,
            4_000.0D,
            4_500.0D,
            5_000.0D,
            5_500.0D,
            6_000.0D,
            6_500.0D,
            7_000.0D,
            7_500.0D,
            8_500.0D,
            10_000.0D,
            12_000.0D,
            14_000.0D,
            25_000.0D,
            30_000.0D,
            35_000.0D,
            40_000.0D,
            45_000.0D,
            50_000.0D,
            60_000.0D,
            70_000.0D,
            80_000.0D,
            90_000.0D,
            100_000.0D,
            200_000.0D,
            250_000.0D,
            300_000.0D,
            350_000.0D,
            500_000.0D
    };

    private LevelTable() {
    }

    public static double requiredExpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return 0.0D;
        }
        int nextLevel = Math.clamp(currentLevel + 1, 1, MAX_LEVEL);
        return REQUIRED_EXP[nextLevel];
    }
}
