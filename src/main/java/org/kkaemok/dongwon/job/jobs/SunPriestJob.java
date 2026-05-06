package org.kkaemok.dongwon.job.jobs;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.kkaemok.dongwon.job.JobType;

import java.util.Map;
import java.util.Set;

public final class SunPriestJob implements JobDefinition {
    private static final Map<PotionEffectType, Integer> DAY_PASSIVE_EFFECTS = Map.of(
            PotionEffectType.STRENGTH, 3,
            PotionEffectType.SPEED, 2,
            PotionEffectType.FIRE_RESISTANCE, 1,
            PotionEffectType.HASTE, 2
    );

    private static final Map<PotionEffectType, Integer> NIGHT_PASSIVE_EFFECTS = Map.of(
            PotionEffectType.STRENGTH, 1,
            PotionEffectType.HASTE, 1
    );

    private static final Set<PotionEffectType> MANAGED_EFFECTS = Set.of(
            PotionEffectType.STRENGTH,
            PotionEffectType.SPEED,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.HASTE
    );

    @Override
    public JobType getType() {
        return JobType.SUN_PRIEST;
    }

    @Override
    public Map<PotionEffectType, Integer> getPassiveEffects(Player player) {
        return isDay(player.getWorld()) ? DAY_PASSIVE_EFFECTS : NIGHT_PASSIVE_EFFECTS;
    }

    @Override
    public Set<PotionEffectType> getManagedPassiveEffects() {
        return MANAGED_EFFECTS;
    }

    private boolean isDay(World world) {
        long time = world.getTime();
        return time >= 0 && time < 12300;
    }
}
