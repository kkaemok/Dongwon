package org.kkaemok.dongwon.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ConfigText {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Supplier<FileConfiguration> configurationSupplier;

    public ConfigText(Plugin plugin) {
        this.configurationSupplier = plugin::getConfig;
    }

    public ConfigText(MessageConfig config) {
        this.configurationSupplier = config::raw;
    }

    public Component component(String path, String fallback, Placeholder... placeholders) {
        return MINI_MESSAGE.deserialize(toMiniMessage(string(path, fallback, placeholders)));
    }

    public Component format(String raw, Placeholder... placeholders) {
        return MINI_MESSAGE.deserialize(toMiniMessage(applyPlaceholders(raw == null ? "" : raw, placeholders)));
    }

    public List<Component> componentList(String path, List<String> fallback, Placeholder... placeholders) {
        FileConfiguration configuration = configurationSupplier.get();
        List<String> rawLines = configuration.isList(path)
                ? configuration.getStringList(path)
                : fallback;
        List<Component> components = new ArrayList<>();
        for (String rawLine : rawLines) {
            components.add(MINI_MESSAGE.deserialize(toMiniMessage(applyPlaceholders(rawLine, placeholders))));
        }
        return components;
    }

    public String string(String path, String fallback, Placeholder... placeholders) {
        String value = configurationSupplier.get().getString(path);
        String resolved = value != null ? value : fallback;
        return applyPlaceholders(resolved == null ? "" : resolved, placeholders);
    }

    public Material material(String path, Material fallback) {
        String rawMaterial = configurationSupplier.get().getString(path);
        if (rawMaterial == null || rawMaterial.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(rawMaterial);
        return material == null ? fallback : material;
    }

    public void send(CommandSender sender, String path, String fallback, Placeholder... placeholders) {
        sender.sendMessage(component(path, fallback, placeholders));
    }

    private String applyPlaceholders(String value, Placeholder... placeholders) {
        String result = value;
        for (Placeholder placeholder : placeholders) {
            result = result.replace("%" + placeholder.key() + "%", placeholder.value());
        }
        return result;
    }

    private String toMiniMessage(String value) {
        String input = value.replace('§', '&');
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current != '&' || i + 1 >= input.length()) {
                builder.append(current);
                continue;
            }

            char code = Character.toLowerCase(input.charAt(i + 1));
            if (code == '#' && i + 7 < input.length()) {
                String hex = input.substring(i + 2, i + 8);
                if (isHexColor(hex)) {
                    builder.append("<reset><#").append(hex).append(">");
                    i += 7;
                    continue;
                }
            }

            if (code == 'x' && i + 13 < input.length()) {
                StringBuilder hex = new StringBuilder(6);
                boolean valid = true;
                for (int offset = 3; offset <= 13; offset += 2) {
                    if (input.charAt(i + offset - 1) != '&' || isNotHex(input.charAt(i + offset))) {
                        valid = false;
                        break;
                    }
                    hex.append(input.charAt(i + offset));
                }
                if (valid) {
                    builder.append("<reset><#").append(hex).append(">");
                    i += 13;
                    continue;
                }
            }

            String tag = legacyTag(code);
            if (tag == null) {
                builder.append(current);
                continue;
            }
            if (isColorCode(code)) {
                builder.append("<reset>");
            }
            builder.append('<').append(tag).append('>');
            i++;
        }
        return builder.toString();
    }

    private boolean isColorCode(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private String legacyTag(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
    }

    private boolean isHexColor(String value) {
        if (value.length() != 6) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (isNotHex(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isNotHex(char value) {
        char lower = Character.toLowerCase(value);
        return !((lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f'));
    }

    public static Placeholder placeholder(String key, Object value) {
        return new Placeholder(key, String.valueOf(value));
    }

    public record Placeholder(String key, String value) {
    }
}
