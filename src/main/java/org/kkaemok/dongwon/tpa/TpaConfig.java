package org.kkaemok.dongwon.tpa;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TpaConfig {
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 60;

    private final MessageConfig config;
    private final ConfigText text;

    public TpaConfig(JavaPlugin plugin, ConfigText text) {
        this.config = new MessageConfig(plugin, "tpa.yml");
        this.text = text;
    }

    public void reload() {
        config.reload();
    }

    public long requestTimeoutMillis() {
        int seconds = Math.max(1, config.getInt("request-timeout-seconds", DEFAULT_REQUEST_TIMEOUT_SECONDS));
        return seconds * 1000L;
    }

    public int requestTimeoutSeconds() {
        return (int) (requestTimeoutMillis() / 1000L);
    }

    public long menuRefreshIntervalTicks() {
        return Math.max(1L, config.getInt("menu.refresh-interval-ticks", 20));
    }

    public Component menuTitle() {
        return text.format(config.getString("menu.title", "<dark_gray>TPA 요청 확인"));
    }

    public Component replyMenuTitle() {
        return text.format(config.getString("reply-menu.title", "<dark_gray>TPA 요청 수락"));
    }

    public Material menuMaterial(String key, Material fallback) {
        String raw = config.getString("menu.items." + key + ".material", fallback.name());
        Material material = Material.matchMaterial(raw);
        return material == null ? fallback : material;
    }

    public Material replyMenuMaterial(String key, Material fallback) {
        String raw = config.getString("reply-menu.items." + key + ".material", fallback.name());
        Material material = Material.matchMaterial(raw);
        return material == null ? fallback : material;
    }

    public Component menuName(String key, String fallback, Map<String, String> placeholders) {
        return text.format(config.getString("menu.items." + key + ".name", fallback), placeholders(placeholders));
    }

    public Component replyMenuName(String key, String fallback, Map<String, String> placeholders) {
        return text.format(config.getString("reply-menu.items." + key + ".name", fallback), placeholders(placeholders));
    }

    public List<Component> menuLore(String key, List<String> fallback, Map<String, String> placeholders) {
        List<String> rawLines = config.getStringList("menu.items." + key + ".lore");
        if (rawLines.isEmpty()) {
            rawLines = fallback;
        }

        List<Component> components = new ArrayList<>();
        for (String rawLine : rawLines) {
            components.add(text.format(rawLine, placeholders(placeholders)));
        }
        return components;
    }

    public List<Component> replyMenuLore(String key, List<String> fallback, Map<String, String> placeholders) {
        List<String> rawLines = config.getStringList("reply-menu.items." + key + ".lore");
        if (rawLines.isEmpty()) {
            rawLines = fallback;
        }

        List<Component> components = new ArrayList<>();
        for (String rawLine : rawLines) {
            components.add(text.format(rawLine, placeholders(placeholders)));
        }
        return components;
    }

    public String regionLabel() {
        return config.getString("menu.region-label", "Asia");
    }

    public Component message(String key, String fallback, ConfigText.Placeholder... placeholders) {
        return text.format(config.getString("messages." + key, fallback), placeholders);
    }

    private ConfigText.Placeholder[] placeholders(Map<String, String> placeholders) {
        return placeholders.entrySet().stream()
                .map(entry -> ConfigText.placeholder(entry.getKey(), entry.getValue()))
                .toArray(ConfigText.Placeholder[]::new);
    }
}
