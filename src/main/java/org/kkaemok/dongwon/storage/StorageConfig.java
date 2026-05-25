package org.kkaemok.dongwon.storage;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.lang.reflect.Field;
import java.util.Locale;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class StorageConfig {
    private final MessageConfig config;
    private final ConfigText text;

    public StorageConfig(JavaPlugin plugin) {
        this.config = new MessageConfig(plugin, "storageconfig.yml");
        this.text = new ConfigText(config);
    }

    public void reload() {
        config.reload();
    }

    public int rows() {
        int rows = config.getInt("settings.rows", 6);
        return Math.max(1, Math.min(6, rows));
    }

    public int size() {
        return rows() * 9;
    }

    public Component title(Player player) {
        return text.component("title", "<dark_gray>창고",
                placeholder("player", player.getName()),
                placeholder("rows", rows()));
    }

    public void send(CommandSender sender, String key, String fallback, ConfigText.Placeholder... placeholders) {
        String raw = config.getString("messages." + key, fallback);
        if (raw == null || raw.isBlank()) {
            return;
        }
        sender.sendMessage(text.format(raw, placeholders));
    }

    public void playSound(Player player, String key) {
        String base = "sounds." + key;
        if (!config.getBoolean(base + ".enabled", true)) {
            return;
        }

        Sound sound = sound(config.getString(base + ".name", ""));
        if (sound == null) {
            return;
        }

        float volume = (float) config.getDouble(base + ".volume", 0.8D);
        float pitch = (float) config.getDouble(base + ".pitch", 1.0D);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private Sound sound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
        if (key != null) {
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) {
                return sound;
            }
        }

        try {
            Field field = Sound.class.getField(normalizeEnum(raw));
            Object value = field.get(null);
            return value instanceof Sound sound ? sound : null;
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            return null;
        }
    }

    private String normalizeEnum(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
