package org.kkaemok.dongwon.job;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JobManager {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, JobType> jobs = new HashMap<>();
    private YamlConfiguration config;

    public JobManager(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "jobs.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public JobType getJob(UUID uuid) {
        return jobs.getOrDefault(uuid, JobType.NONE);
    }

    public void setJob(UUID uuid, JobType jobType) {
        jobs.put(uuid, jobType);
        config.set("players." + uuid, jobType.getKey());
        save();
    }

    public void load() {
        jobs.clear();
        this.config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("players") == null) {
            return;
        }
        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String raw = config.getString("players." + key, "none");
                JobType jobType = JobType.fromInput(raw).orElse(JobType.NONE);
                jobs.put(uuid, jobType);
            } catch (IllegalArgumentException ignored) {
                // invalid uuid key; ignore silently
            }
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("jobs.yml 저장 실패: " + e.getMessage());
        }
    }
}
