package org.kkaemok.dongwon.guild;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class GuildColor {
    private static final Map<String, String> COLOR_MAP = new HashMap<>();

    static {
        register("0", "black", "검정", "검은색");
        register("1", "darkblue", "짙은파랑");
        register("2", "darkgreen", "짙은초록");
        register("3", "darkaqua", "짙은청록");
        register("4", "darkred", "짙은빨강");
        register("5", "darkpurple", "짙은보라");
        register("6", "gold", "주황", "금색");
        register("7", "gray", "회색");
        register("8", "darkgray", "짙은회색");
        register("9", "blue", "파랑", "파란색");
        register("a", "green", "연두", "연두색", "초록", "초록색");
        register("b", "aqua", "하늘", "하늘색", "청록");
        register("c", "red", "빨강", "빨간색");
        register("d", "lightpurple", "보라", "보라색", "핑크");
        register("e", "yellow", "노랑", "노란색");
        register("f", "white", "흰색", "하양");
    }

    private GuildColor() {
    }

    public static Optional<String> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String raw = input.trim().toLowerCase(Locale.ROOT);
        if (raw.length() == 2 && (raw.charAt(0) == '&' || raw.charAt(0) == '§') && isColorCode(raw.charAt(1))) {
            return Optional.of("§" + raw.charAt(1));
        }
        if (raw.length() == 1 && isColorCode(raw.charAt(0))) {
            return Optional.of("§" + raw);
        }
        return Optional.ofNullable(COLOR_MAP.get(raw));
    }

    public static String examples() {
        return "빨강, 노랑, 연두, 하늘, 보라, 흰색, 회색, &c, &e, &a";
    }

    private static void register(String code, String... keys) {
        String value = "§" + code;
        COLOR_MAP.put(code, value);
        COLOR_MAP.put("§" + code, value);
        COLOR_MAP.put("&" + code, value);
        for (String key : keys) {
            COLOR_MAP.put(normalize(key), value);
        }
    }

    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
