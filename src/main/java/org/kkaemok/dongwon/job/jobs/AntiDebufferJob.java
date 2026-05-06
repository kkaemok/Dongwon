package org.kkaemok.dongwon.job.jobs;

import org.bukkit.potion.PotionEffectType;
import org.kkaemok.dongwon.job.JobType;

import java.util.Map;

public final class AntiDebufferJob implements JobDefinition {
    private static final Map<PotionEffectType, Integer> IMMUNITY_LEVELS = Map.of(
            PotionEffectType.POISON, 10,
            PotionEffectType.WEAKNESS, 5,
            PotionEffectType.HUNGER, 10,
            PotionEffectType.NAUSEA, 5,
            PotionEffectType.MINING_FATIGUE, 5,
            PotionEffectType.SLOWNESS, 5
    );

    @Override
    public JobType getType() {
        return JobType.ANTI_DEBUFFER;
    }

    @Override
    public boolean isDebuffImmune(PotionEffectType type, int level) {
        Integer maxLevel = IMMUNITY_LEVELS.get(type);
        return maxLevel != null && level <= maxLevel;
    }
}
