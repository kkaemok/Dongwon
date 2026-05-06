package org.kkaemok.dongwon.progression;

import org.bukkit.entity.Player;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobType;

import java.util.concurrent.ThreadLocalRandom;

public final class MasteryService {
    private final ProfileManager profileManager;
    private final JobManager jobManager;

    public MasteryService(ProfileManager profileManager, JobManager jobManager) {
        this.profileManager = profileManager;
        this.jobManager = jobManager;
    }

    public void onMonsterKill(Player player) {
        if (!isMasteryEnabled(player)) {
            return;
        }
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        int gain = ThreadLocalRandom.current().nextInt(1, 11);
        int previousLevel = MasteryLevel.resolveLevelByExp(profile.getMasteryExp());
        profile.addMasteryExp(gain);
        int newLevel = MasteryLevel.resolveLevelByExp(profile.getMasteryExp());
        profile.setMasteryLevel(newLevel);

        player.sendMessage("숙련도 +" + gain + " (현재 " + profile.getMasteryExp() + ")");

        if (newLevel > previousLevel) {
            for (int level = previousLevel + 1; level <= newLevel; level++) {
                grantLevelReward(player, profile, MasteryLevel.byLevel(level));
            }
        }

        profileManager.save();
    }

    public PlayerProfile getProfile(Player player) {
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        int resolvedLevel = MasteryLevel.resolveLevelByExp(profile.getMasteryExp());
        if (profile.getMasteryLevel() != resolvedLevel) {
            profile.setMasteryLevel(resolvedLevel);
            profileManager.save();
        }
        return profile;
    }

    public boolean hasJinSwordsman(Player player) {
        return isMasteryEnabled(player)
                && getProfile(player).getSpecialization() == MasterySpecialization.JIN_SWORDSMAN;
    }

    public boolean hasSpecialization(Player player, MasterySpecialization specialization) {
        if (specialization == null || specialization == MasterySpecialization.NONE) {
            return false;
        }
        return isMasteryEnabled(player) && getProfile(player).getSpecialization() == specialization;
    }

    public boolean isMasteryEnabled(Player player) {
        return jobManager.getJob(player.getUniqueId()) == JobType.SWORDSMAN;
    }

    public boolean chooseSpecialization(Player player, MasterySpecialization specialization) {
        if (!isMasteryEnabled(player)) {
            player.sendMessage("숙련도 시스템은 검사 직업에서만 사용할 수 있습니다.");
            return false;
        }
        if (specialization == null || specialization == MasterySpecialization.NONE) {
            player.sendMessage("선택 가능한 특화: 시현류, 진-검사");
            return false;
        }
        PlayerProfile profile = getProfile(player);
        if (profile.getMasteryLevel() < 3) {
            player.sendMessage("특화 선택은 숙련도 레벨 3부터 가능합니다.");
            return false;
        }
        if (profile.getSpecialization() == specialization) {
            player.sendMessage("이미 선택한 특화입니다: " + specialization.getDisplayName());
            return true;
        }
        profile.setSpecialization(specialization);
        profileManager.save();
        player.sendMessage("특화가 선택되었습니다: " + specialization.getDisplayName());
        return true;
    }

    private void grantLevelReward(Player player, PlayerProfile profile, MasteryLevel level) {
        if (level.getCanReward() > 0) {
            profile.addCan(level.getCanReward());
        }
        if (level.getSilverCanReward() > 0) {
            profile.addSilverCan(level.getSilverCanReward());
        }

        if (level.getLevel() == 3 && profile.getSpecialization() == MasterySpecialization.NONE) {
            player.sendMessage("숙련도 레벨 3 달성! /숙련도 선택 <시현류|진-검사> 로 특화를 선택하세요.");
        }

        player.sendMessage(
                "숙련도 레벨 " + level.getLevel() + " 보상 획득: 캔 " + level.getCanReward()
                        + ", 실버캔 " + level.getSilverCanReward()
        );
    }

    public void save() {
        profileManager.save();
    }
}
