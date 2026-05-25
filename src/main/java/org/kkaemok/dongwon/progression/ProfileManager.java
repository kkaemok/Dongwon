package org.kkaemok.dongwon.progression;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.job.JobType;
import org.kkaemok.dongwon.land.LandClaim;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    public Collection<UUID> knownPlayerIds() {
        return List.copyOf(profiles.keySet());
    }

    public void setPlayerName(UUID playerId, String playerName) {
        get(playerId).setPlayerName(playerName);
    }

    public JobType getJob(UUID playerId) {
        return get(playerId).getJobType();
    }

    public void setJob(UUID playerId, JobType jobType) {
        get(playerId).setJobType(jobType);
        save();
    }

    public boolean isTpaGuiEnabled(UUID playerId, boolean fallback) {
        String path = "players." + playerId + ".settings.tpa-gui";
        return config.getBoolean(path, fallback);
    }

    public boolean toggleTpaGui(UUID playerId, boolean fallback) {
        boolean enabled = !isTpaGuiEnabled(playerId, fallback);
        config.set("players." + playerId + ".settings.tpa-gui", enabled);
        save();
        return enabled;
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
                profile.setPlayerName(config.getString(base + ".name", ""));
                profile.setJobType(JobType.fromInput(config.getString(base + ".job.current", "none")).orElse(JobType.NONE));
                profile.addCan(config.getLong(base + ".currency.can", 0L));
                profile.addSilverCan(config.getLong(base + ".currency.silver_can", 0L));
                profile.setGoldenCan(config.getLong(base + ".currency.golden_can", 0L));
                profile.setLevel(config.getInt(base + ".level.level", 0));
                profile.setLevelExp(config.getDouble(base + ".level.exp", 0.0D));
                profile.addSwordsmanMasteryExp(config.getLong(
                        base + ".mastery.swordsman.exp",
                        config.getLong(base + ".mastery.exp", 0L)
                ));
                profile.setSwordsmanMasteryLevel(config.getInt(
                        base + ".mastery.swordsman.level",
                        config.getInt(base + ".mastery.level", 0)
                ));
                profile.setSwordsmanSpecialization(MasterySpecialization.fromKey(
                        config.getString(
                                base + ".mastery.swordsman.specialization",
                                config.getString(base + ".mastery.specialization", "none")
                        )
                ));
                profile.addFishermanMasteryExp(config.getLong(base + ".mastery.fisherman.exp", 0L));
                profile.setGuildName(config.getString(base + ".guild.name", "미가입"));
                profiles.put(playerId, profile);
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, PlayerProfile> entry : profiles.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerProfile profile = entry.getValue();
            String base = "players." + playerId;
            config.set(base + ".name", profile.getPlayerName());
            config.set(base + ".job.current", profile.getJobType().getKey());
            config.set(base + ".currency.can", profile.getCan());
            config.set(base + ".currency.silver_can", profile.getSilverCan());
            config.set(base + ".currency.golden_can", profile.getGoldenCan());
            config.set(base + ".level.level", profile.getLevel());
            config.set(base + ".level.exp", profile.getLevelExp());
            config.set(base + ".mastery.swordsman.exp", profile.getSwordsmanMasteryExp());
            config.set(base + ".mastery.swordsman.level", profile.getSwordsmanMasteryLevel());
            config.set(base + ".mastery.swordsman.specialization", profile.getSwordsmanSpecialization().getKey());
            config.set(base + ".mastery.fisherman.exp", profile.getFishermanMasteryExp());
            config.set(base + ".guild.name", profile.getGuildName());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("profiles.yml 저장 실패: " + e.getMessage());
        }
    }

    public List<LandClaim> loadLandClaims() {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return List.of();
        }

        Map<String, LandClaim> claims = new LinkedHashMap<>();
        for (String ownerRaw : players.getKeys(false)) {
            UUID ownerId;
            try {
                ownerId = UUID.fromString(ownerRaw);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            String ownerName = config.getString("players." + ownerRaw + ".name", ownerRaw);
            ConfigurationSection lands = config.getConfigurationSection("players." + ownerRaw + ".lands");
            if (lands == null) {
                continue;
            }

            for (String key : lands.getKeys(false)) {
                String base = "players." + ownerRaw + ".lands." + key;
                try {
                    Map<UUID, String> members = new LinkedHashMap<>();
                    ConfigurationSection memberSection = config.getConfigurationSection(base + ".members");
                    if (memberSection != null) {
                        for (String memberRaw : memberSection.getKeys(false)) {
                            UUID memberId = UUID.fromString(memberRaw);
                            members.put(memberId, config.getString(base + ".members." + memberRaw + ".name", memberId.toString()));
                        }
                    }

                    LandClaim claim = new LandClaim(
                            key,
                            config.getString(base + ".name", key),
                            ownerId,
                            config.getString(base + ".owner-name", ownerName),
                            config.getString(base + ".world", ""),
                            config.getInt(base + ".min-x"),
                            config.getInt(base + ".max-x"),
                            config.getInt(base + ".min-z"),
                            config.getInt(base + ".max-z"),
                            parseCreatedAt(config.getString(base + ".created-at", "")),
                            members
                    );
                    claims.put(claim.getKey(), claim);
                } catch (IllegalArgumentException ignored) {
                    // skip malformed land entry
                }
            }
        }
        return List.copyOf(claims.values());
    }

    public void saveLandClaims(Collection<LandClaim> claims) {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players != null) {
            for (String playerId : players.getKeys(false)) {
                config.set("players." + playerId + ".lands", null);
            }
        }

        for (LandClaim claim : claims) {
            UUID ownerId = claim.getOwnerId();
            PlayerProfile profile = get(ownerId);
            if (profile.getPlayerName().isBlank()) {
                profile.setPlayerName(claim.getOwnerName());
            }

            String base = "players." + ownerId + ".lands." + claim.getKey();
            config.set(base + ".name", claim.getName());
            config.set(base + ".world", claim.getWorldName());
            config.set(base + ".owner-name", claim.getOwnerName());
            config.set(base + ".min-x", claim.getMinX());
            config.set(base + ".max-x", claim.getMaxX());
            config.set(base + ".min-z", claim.getMinZ());
            config.set(base + ".max-z", claim.getMaxZ());
            config.set(base + ".created-at", Instant.ofEpochMilli(claim.getCreatedAt()).toString());
            config.set(base + ".members", null);
            for (Map.Entry<UUID, String> entry : claim.getMembers().entrySet()) {
                config.set(base + ".members." + entry.getKey() + ".name", entry.getValue());
            }
        }
        save();
    }

    public void migrateJob(UUID playerId, JobType jobType) {
        PlayerProfile profile = get(playerId);
        if (profile.getJobType() == JobType.NONE && jobType != JobType.NONE) {
            profile.setJobType(jobType);
        }
    }

    public void migrateTpaGuiSettings(File file) {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = legacy.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String legacyPath = "players." + key + ".tpa-gui";
                String profilePath = "players." + playerId + ".settings.tpa-gui";
                if (legacy.contains(legacyPath) && !config.contains(profilePath)) {
                    config.set(profilePath, legacy.getBoolean(legacyPath, true));
                    get(playerId);
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID
            }
        }
        save();
    }

    private long parseCreatedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (RuntimeException ignored) {
            return System.currentTimeMillis();
        }
    }
}
