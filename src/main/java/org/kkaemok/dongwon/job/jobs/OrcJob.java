package org.kkaemok.dongwon.job.jobs;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.kkaemok.dongwon.job.JobType;

import java.util.Map;
import java.util.Set;

public final class OrcJob implements JobDefinition {
    private static final Map<PotionEffectType, Integer> PASSIVE_EFFECTS = Map.of(
            PotionEffectType.STRENGTH, 1
    );

    @Override
    public JobType getType() {
        return JobType.ORC;
    }

    @Override
    public Map<PotionEffectType, Integer> getPassiveEffects(Player player) {
        return PASSIVE_EFFECTS;
    }

    @Override
    public Set<PotionEffectType> getManagedPassiveEffects() {
        return PASSIVE_EFFECTS.keySet();
    }

    @Override
    public double getMaxHealthBonus() {
        return 20.0D;
    }
}
