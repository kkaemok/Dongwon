package org.kkaemok.dongwon.level;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.kkaemok.dongwon.progression.PlayerProfile;
import org.kkaemok.dongwon.progression.ProfileManager;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class LevelService {
    private final ProfileManager profileManager;
    private final BossBarConfig config;
    private final LevelBossBarManager bossBarManager;

    public LevelService(ProfileManager profileManager, BossBarConfig config, LevelBossBarManager bossBarManager) {
        this.profileManager = profileManager;
        this.config = config;
        this.bossBarManager = bossBarManager;
    }

    public double getKillExp(Entity entity) {
        EntityType type = entity.getType();
        if (type == EntityType.WARDEN) {
            return 100.0D;
        }
        if (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON) {
            return 50.0D;
        }
        if (entity instanceof Monster) {
            return 2.0D;
        }
        return 0.0D;
    }

    public void addExp(Player player, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        PlayerProfile profile = profileManager.get(player.getUniqueId());
        int previousLevel = profile.getLevel();
        if (previousLevel >= LevelTable.MAX_LEVEL) {
            showMaxLevel(player, amount);
            return;
        }

        profile.setLevelExp(profile.getLevelExp() + amount);
        while (profile.getLevel() < LevelTable.MAX_LEVEL) {
            double required = LevelTable.requiredExpForNextLevel(profile.getLevel());
            if (profile.getLevelExp() < required) {
                break;
            }
            profile.setLevelExp(profile.getLevelExp() - required);
            profile.setLevel(profile.getLevel() + 1);
        }

        if (profile.getLevel() >= LevelTable.MAX_LEVEL) {
            profile.setLevel(LevelTable.MAX_LEVEL);
            profile.setLevelExp(0.0D);
        }

        profileManager.save();
        if (profile.getLevel() > previousLevel) {
            showLevelUp(player, profile, amount);
            return;
        }
        showExpGain(player, profile, amount);
    }

    public void clearBossBar(Player player) {
        bossBarManager.clear(player);
    }

    public void shutdown() {
        bossBarManager.shutdown();
    }

    private void showExpGain(Player player, PlayerProfile profile, double gain) {
        double required = LevelTable.requiredExpForNextLevel(profile.getLevel());
        Component title = config.message("exp-gain",
                "<green>[Lv. %level%] <white>다음 Lv. %next_level%까지 %exp% / %required_exp% XP <gray>(+%gain%)",
                placeholder("level", profile.getLevel()),
                placeholder("next_level", profile.getLevel() + 1),
                placeholder("exp", LevelExpFormatter.format(profile.getLevelExp())),
                placeholder("required_exp", LevelExpFormatter.format(required)),
                placeholder("gain", LevelExpFormatter.format(gain)));
        bossBarManager.show(player, title, progress(profile));
    }

    private void showLevelUp(Player player, PlayerProfile profile, double gain) {
        if (profile.getLevel() >= LevelTable.MAX_LEVEL) {
            showMaxLevel(player, gain);
            return;
        }

        double required = LevelTable.requiredExpForNextLevel(profile.getLevel());
        Component title = config.message("level-up",
                "<gold>[Lv. %level%] 레벨 업! <white>다음 Lv. %next_level%까지 %exp% / %required_exp% XP",
                placeholder("level", profile.getLevel()),
                placeholder("next_level", profile.getLevel() + 1),
                placeholder("exp", LevelExpFormatter.format(profile.getLevelExp())),
                placeholder("required_exp", LevelExpFormatter.format(required)),
                placeholder("gain", LevelExpFormatter.format(gain)));
        bossBarManager.show(player, title, progress(profile));
    }

    private void showMaxLevel(Player player, double gain) {
        Component title = config.message("max-level",
                "<gold>[Lv. 50] 최고 레벨입니다 <gray>(+%gain%)",
                placeholder("level", LevelTable.MAX_LEVEL),
                placeholder("next_level", LevelTable.MAX_LEVEL),
                placeholder("exp", LevelExpFormatter.format(0.0D)),
                placeholder("required_exp", LevelExpFormatter.format(0.0D)),
                placeholder("gain", LevelExpFormatter.format(gain)));
        bossBarManager.show(player, title, 1.0F);
    }

    private float progress(PlayerProfile profile) {
        if (profile.getLevel() >= LevelTable.MAX_LEVEL) {
            return 1.0F;
        }
        double required = LevelTable.requiredExpForNextLevel(profile.getLevel());
        if (required <= 0.0D) {
            return 1.0F;
        }
        return (float) (profile.getLevelExp() / required);
    }
}
