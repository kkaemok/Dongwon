package org.kkaemok.dongwon.level;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.util.Locale;

public final class BossBarConfig {
    private final MessageConfig config;
    private final ConfigText text;

    public BossBarConfig(JavaPlugin plugin, ConfigText text) {
        this.config = new MessageConfig(plugin, "bossbar.yml");
        this.text = text;
    }

    public void reload() {
        config.reload();
    }

    public long durationTicks() {
        return Math.max(1, config.getInt("duration-seconds", 4)) * 20L;
    }

    public BossBar.Color color() {
        return parseColor(config.getString("color", "GREEN"));
    }

    public BossBar.Overlay overlay() {
        return parseOverlay(config.getString("overlay", "PROGRESS"));
    }

    public Component message(String key, String fallback, ConfigText.Placeholder... placeholders) {
        return text.format(config.getString("messages." + key, fallback), placeholders);
    }

    private BossBar.Color parseColor(String raw) {
        try {
            return BossBar.Color.valueOf(normalize(raw));
        } catch (IllegalArgumentException ex) {
            return BossBar.Color.GREEN;
        }
    }

    private BossBar.Overlay parseOverlay(String raw) {
        try {
            return BossBar.Overlay.valueOf(normalize(raw));
        } catch (IllegalArgumentException ex) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
