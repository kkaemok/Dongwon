package org.kkaemok.dongwon.progression;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProfileManager {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private YamlConfiguration config;

    public ProfileManager(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "profiles.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public PlayerProfile get(UUID playerId) {
        return profiles.computeIfAbsent(playerId, ignored -> new PlayerProfile());
    }

    public void load() {
        profiles.clear();
        this.config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String base = "players." + key;
                PlayerProfile profile = new PlayerProfile();
                profile.addCan(config.getLong(base + ".currency.can", 0L));
                profile.addSilverCan(config.getLong(base + ".currency.silver_can", 0L));
                profile.setGoldenCan(config.getLong(base + ".currency.golden_can", 0L));
                profile.addMasteryExp(config.getLong(base + ".mastery.exp", 0L));
                profile.setMasteryLevel(config.getInt(base + ".mastery.level", 0));
                profile.setSpecialization(MasterySpecialization.fromKey(
                        config.getString(base + ".mastery.specialization", "none")
                ));
                profile.setGuildName(config.getString(base + ".guild.name", "미가입"));
                profiles.put(playerId, profile);
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID
            }
        }
    }

    public void save() {
        config.set("players", null);
        for (Map.Entry<UUID, PlayerProfile> entry : profiles.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerProfile profile = entry.getValue();
            String base = "players." + playerId;
            config.set(base + ".currency.can", profile.getCan());
            config.set(base + ".currency.silver_can", profile.getSilverCan());
            config.set(base + ".currency.golden_can", profile.getGoldenCan());
            config.set(base + ".mastery.exp", profile.getMasteryExp());
            config.set(base + ".mastery.level", profile.getMasteryLevel());
            config.set(base + ".mastery.specialization", profile.getSpecialization().getKey());
            config.set(base + ".guild.name", profile.getGuildName());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("profiles.yml 저장 실패: " + e.getMessage());
        }
    }
}
