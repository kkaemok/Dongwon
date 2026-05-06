package org.kkaemok.dongwon.job.jobs;

import org.bukkit.potion.PotionEffectType;
import org.kkaemok.dongwon.job.JobType;

import java.util.Set;

public final class ShinnongJob implements JobDefinition {
    private static final Set<PotionEffectType> IMMUNITIES = Set.of(
            PotionEffectType.POISON,
            PotionEffectType.WEAKNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.NAUSEA,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WITHER,
            PotionEffectType.BLINDNESS,
            PotionEffectType.DARKNESS,
            PotionEffectType.LEVITATION,
            PotionEffectType.BAD_OMEN,
            PotionEffectType.RAID_OMEN,
            PotionEffectType.UNLUCK,
            PotionEffectType.INFESTED,
            PotionEffectType.WEAVING,
            PotionEffectType.OOZING
    );

    @Override
    public JobType getType() {
        return JobType.SHINNONG;
    }

    @Override
    public boolean isDebuffImmune(PotionEffectType type, int level) {
        return IMMUNITIES.contains(type);
    }
}
