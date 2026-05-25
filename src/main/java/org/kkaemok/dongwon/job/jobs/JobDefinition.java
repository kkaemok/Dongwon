package org.kkaemok.dongwon.job.jobs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import org.kkaemok.dongwon.job.JobType;

import java.util.Map;
import java.util.Set;

public interface JobDefinition {
    JobType getType();

    default Map<PotionEffectType, Integer> getPassiveEffects(Player player) {
        return Map.of();
    }

    default Set<PotionEffectType> getManagedPassiveEffects() {
        return Set.of();
    }

    default double getMaxHealthBonus() {
        return 0.0D;
    }

    default boolean isDebuffImmune(PotionEffectType type, int level) {
        return false;
    }

    default @Nullable ItemStack rollFishingLoot(long fishingMasteryExp) {
        return null;
    }
}
