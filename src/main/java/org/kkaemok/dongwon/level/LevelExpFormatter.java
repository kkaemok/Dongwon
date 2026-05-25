package org.kkaemok.dongwon.level;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class LevelExpFormatter {
    private LevelExpFormatter() {
    }

    public static String format(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.#");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(Math.max(0.0D, value));
    }
}
