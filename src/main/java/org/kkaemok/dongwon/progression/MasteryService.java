package org.kkaemok.dongwon.progression;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobType;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class MasteryService {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    private final ProfileManager profileManager;
    private final JobManager jobManager;
    private final MasteryConfig config;

    public MasteryService(ProfileManager profileManager, JobManager jobManager, MasteryConfig config) {
        this.profileManager = profileManager;
        this.jobManager = jobManager;
        this.config = config;
    }

    public void onMonsterKill(Player player) {
        if (!isSwordsmanMasteryEnabled(player)) {
            return;
        }
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        int gain = ThreadLocalRandom.current().nextInt(1, 11);
        int previousLevel = SwordsmanMasteryLevel.resolveLevelByExp(profile.getSwordsmanMasteryExp());
        profile.addSwordsmanMasteryExp(gain);
        int newLevel = SwordsmanMasteryLevel.resolveLevelByExp(profile.getSwordsmanMasteryExp());
        profile.setSwordsmanMasteryLevel(newLevel);

        config.send(player, "swordsman.exp-gain", "<green>검사 숙련도 +%gain% <gray>(현재 %exp%)",
                placeholder("gain", gain),
                placeholder("exp", format(profile.getSwordsmanMasteryExp())));

        if (newLevel > previousLevel) {
            for (int level = previousLevel + 1; level <= newLevel; level++) {
                grantSwordsmanLevelReward(player, profile, SwordsmanMasteryLevel.byLevel(level));
            }
        }

        profileManager.save();
    }

    public long onFishing(Player player) {
        if (jobManager.getJob(player.getUniqueId()) != JobType.FISHERMAN) {
            return 0L;
        }

        PlayerProfile profile = profileManager.get(player.getUniqueId());
        FishermanMasteryLevel previousLevel = FishermanMasteryLevel.resolveByExp(profile.getFishermanMasteryExp());
        profile.addFishermanMasteryExp(1L);
        long exp = profile.getFishermanMasteryExp();
        FishermanMasteryLevel newLevel = FishermanMasteryLevel.resolveByExp(exp);

        config.send(player, "fisherman.exp-gain", "<aqua>낚시 숙련도 +1 <gray>(현재 %exp%)",
                placeholder("exp", format(exp)));

        if (newLevel != previousLevel) {
            config.send(player, "fisherman.tier-up", "<gold>낚시꾼 숙련도 %tier% 달성!",
                    placeholder("tier", newLevel.getDisplayName()),
                    placeholder("exp", format(exp)));
        }

        profileManager.save();
        return exp;
    }

    public PlayerProfile getProfile(Player player) {
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        int resolvedLevel = SwordsmanMasteryLevel.resolveLevelByExp(profile.getSwordsmanMasteryExp());
        if (profile.getSwordsmanMasteryLevel() != resolvedLevel) {
            profile.setSwordsmanMasteryLevel(resolvedLevel);
            profileManager.save();
        }
        return profile;
    }

    public void sendMasteryInfo(Player player) {
        JobType jobType = jobManager.getJob(player.getUniqueId());
        if (jobType == JobType.SWORDSMAN) {
            sendSwordsmanInfo(player);
            return;
        }
        if (jobType == JobType.FISHERMAN) {
            sendFishermanInfo(player);
            return;
        }

        config.send(player, "common.unsupported-job", "<red>현재 직업은 숙련도 정보가 없습니다.");
    }

    public void sendPlayerOnly(CommandSender sender) {
        config.send(sender, "common.player-only", "<red>플레이어만 사용할 수 있습니다.");
    }

    public void sendSwordsmanSelectUsage(CommandSender sender) {
        config.send(sender, "swordsman.select-usage", "<yellow>사용법: /숙련도 선택 <시현류|진-검사>");
    }

    public boolean hasJinSwordsman(Player player) {
        return isSwordsmanMasteryEnabled(player)
                && getProfile(player).getSwordsmanSpecialization() == MasterySpecialization.JIN_SWORDSMAN;
    }

    public boolean hasSpecialization(Player player, MasterySpecialization specialization) {
        if (specialization == null || specialization == MasterySpecialization.NONE) {
            return false;
        }
        return isSwordsmanMasteryEnabled(player) && getProfile(player).getSwordsmanSpecialization() == specialization;
    }

    public boolean isMasteryEnabled(Player player) {
        return isSwordsmanMasteryEnabled(player);
    }

    public void chooseSpecialization(Player player, MasterySpecialization specialization) {
        if (!isSwordsmanMasteryEnabled(player)) {
            config.send(player, "swordsman.only", "<red>검사 직업에서만 특화를 선택할 수 있습니다.");
            return;
        }
        if (specialization == null || specialization == MasterySpecialization.NONE) {
            config.send(player, "swordsman.specialization-options", "<yellow>선택 가능한 특화: 시현류, 진-검사");
            return;
        }
        PlayerProfile profile = getProfile(player);
        if (profile.getSwordsmanMasteryLevel() < 3) {
            config.send(player, "swordsman.specialization-level-required", "<red>특화 선택은 검사 숙련도 레벨 3부터 가능합니다.");
            return;
        }
        if (profile.getSwordsmanSpecialization() == specialization) {
            config.send(player, "swordsman.specialization-already-selected", "<red>이미 선택한 특화입니다: %specialization%",
                    placeholder("specialization", specialization.getDisplayName()));
            return;
        }
        profile.setSwordsmanSpecialization(specialization);
        profileManager.save();
        config.send(player, "swordsman.specialization-selected", "<green>특화가 선택되었습니다: %specialization%",
                placeholder("specialization", specialization.getDisplayName()));
    }

    private boolean isSwordsmanMasteryEnabled(Player player) {
        return jobManager.getJob(player.getUniqueId()) == JobType.SWORDSMAN;
    }

    private void sendSwordsmanInfo(Player player) {
        PlayerProfile profile = getProfile(player);
        config.sendLines(player, "swordsman.info", List.of(
                        "<yellow>검사 숙련도",
                        "<white>레벨: %level%",
                        "<white>경험치: %exp%",
                        "<white>특화: %specialization%",
                        "<white>캔: %can%",
                        "<white>실버캔: %silver_can%",
                        "<white>황금캔: %golden_can%"
                ),
                placeholder("level", profile.getSwordsmanMasteryLevel()),
                placeholder("exp", format(profile.getSwordsmanMasteryExp())),
                placeholder("specialization", profile.getSwordsmanSpecialization().getDisplayName()),
                placeholder("can", format(profile.getCan())),
                placeholder("silver_can", format(profile.getSilverCan())),
                placeholder("golden_can", format(profile.getGoldenCan())));

        if (profile.getSwordsmanMasteryLevel() >= 3
                && profile.getSwordsmanSpecialization() == MasterySpecialization.NONE) {
            config.send(player, "swordsman.specialization-hint", "<yellow>/숙련도 선택 <시현류|진-검사> 로 특화를 선택하세요.");
        }
    }

    private void sendFishermanInfo(Player player) {
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        long exp = profile.getFishermanMasteryExp();
        FishermanMasteryLevel level = FishermanMasteryLevel.resolveByExp(exp);
        FishermanMasteryLevel next = level.next();
        String nextExp = next == null ? "최고 단계" : format(next.getRequiredExp());
        String remainingExp = next == null ? "0" : format(Math.max(0L, next.getRequiredExp() - exp));

        config.sendLines(player, "fisherman.info", List.of(
                        "<aqua>낚시꾼 숙련도",
                        "<white>현재 숙련도: %exp%",
                        "<white>희귀 보상 단계: %tier%",
                        "<white>다음 단계: %next_exp%",
                        "<white>남은 숙련도: %remaining_exp%"
                ),
                placeholder("exp", format(exp)),
                placeholder("tier", level.getDisplayName()),
                placeholder("next_exp", nextExp),
                placeholder("remaining_exp", remainingExp));
    }

    private void grantSwordsmanLevelReward(Player player, PlayerProfile profile, SwordsmanMasteryLevel level) {
        if (level.getCanReward() > 0) {
            profile.addCan(level.getCanReward());
        }
        if (level.getSilverCanReward() > 0) {
            profile.addSilverCan(level.getSilverCanReward());
        }

        if (level.getLevel() == 3 && profile.getSwordsmanSpecialization() == MasterySpecialization.NONE) {
            config.send(player, "swordsman.level-three-specialization",
                    "<yellow>검사 숙련도 레벨 3 달성! /숙련도 선택 <시현류|진-검사> 로 특화를 선택하세요.");
        }

        config.send(player, "swordsman.level-reward",
                "<green>검사 숙련도 레벨 %level% 보상 획득: 캔 %can%, 실버캔 %silver_can%",
                placeholder("level", level.getLevel()),
                placeholder("can", format(level.getCanReward())),
                placeholder("silver_can", format(level.getSilverCanReward())));
    }

    private String format(long value) {
        return NUMBER_FORMAT.format(Math.max(0L, value));
    }

    public void save() {
        profileManager.save();
    }
}
