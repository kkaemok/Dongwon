package org.kkaemok.dongwon.rtp;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RtpConfig {
    private static final int DEFAULT_COUNTDOWN_SECONDS = 3;
    private static final int DEFAULT_MIN_DISTANCE = 500;
    private static final int DEFAULT_MAX_DISTANCE = 5_000;
    private static final int DEFAULT_MAX_ATTEMPTS = 80;

    private final MessageConfig config;
    private final ConfigText text;
    private Set<Material> bannedGroundBlocks;

    public RtpConfig(JavaPlugin plugin, ConfigText text) {
        this.config = new MessageConfig(plugin, "rtp.yml");
        this.text = text;
        this.bannedGroundBlocks = parseBannedGroundBlocks();
    }

    public void reload() {
        config.reload();
        this.bannedGroundBlocks = parseBannedGroundBlocks();
    }

    public int countdownSeconds() {
        return Math.max(1, config.getInt("countdown-seconds", DEFAULT_COUNTDOWN_SECONDS));
    }

    public int minDistance() {
        return Math.max(0, config.getInt("min-distance", DEFAULT_MIN_DISTANCE));
    }

    public int maxDistance(World world) {
        int legacyRange = range(world);
        int defaultMaxDistance = Math.max(1, config.getInt("max-distance", legacyRange));
        String worldPath = "range-by-world." + world.getName();
        return Math.max(minDistance(), config.getInt(worldPath, defaultMaxDistance));
    }

    public int maxAttempts() {
        return Math.max(1, config.getInt("max-attempts", DEFAULT_MAX_ATTEMPTS));
    }

    public boolean isBannedGround(Material material) {
        return bannedGroundBlocks.contains(material);
    }

    public Component message(String key, String fallback, ConfigText.Placeholder... placeholders) {
        return text.format(config.getString("messages." + key, fallback), placeholders);
    }

    public int range(World world) {
        int defaultRange = Math.max(1, config.getInt("default-range", DEFAULT_MAX_DISTANCE));
        String worldPath = "range-by-world." + world.getName();
        return Math.max(1, config.getInt(worldPath, defaultRange));
    }

    private Set<Material> parseBannedGroundBlocks() {
        LinkedHashSet<Material> materials = new LinkedHashSet<>();
        for (String raw : config.getStringList("banned-ground-blocks")) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                materials.add(material);
            }
        }
        return Collections.unmodifiableSet(materials);
    }
}
