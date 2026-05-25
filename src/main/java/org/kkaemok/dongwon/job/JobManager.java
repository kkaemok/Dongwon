package org.kkaemok.dongwon.job;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.progression.ProfileManager;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public final class JobManager {
    private final JavaPlugin plugin;
    private final ProfileManager profileManager;

    public JobManager(JavaPlugin plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        migrateLegacyJobs();
    }

    public JobType getJob(UUID uuid) {
        return profileManager.getJob(uuid);
    }

    public void setJob(UUID uuid, JobType jobType) {
        profileManager.setJob(uuid, jobType);
    }

    private void migrateLegacyJobs() {
        File file = new File(plugin.getDataFolder(), "jobs.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("players") == null) {
            return;
        }
        for (String key : Objects.requireNonNull(config.getConfigurationSection("players")).getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String raw = config.getString("players." + key, "none");
                JobType jobType = JobType.fromInput(raw).orElse(JobType.NONE);
                profileManager.migrateJob(uuid, jobType);
            } catch (IllegalArgumentException ignored) {
                // invalid uuid key; ignore silently
            }
        }
        profileManager.save();
    }

    public void save() {
        profileManager.save();
    }
}
