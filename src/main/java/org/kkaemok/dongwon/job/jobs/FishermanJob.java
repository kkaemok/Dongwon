package org.kkaemok.dongwon.job.jobs;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.kkaemok.dongwon.job.JobType;

import java.util.concurrent.ThreadLocalRandom;

public final class FishermanJob implements JobDefinition {
    @Override
    public JobType getType() {
        return JobType.FISHERMAN;
    }

    @Override
    public @Nullable ItemStack rollFishingLoot() {
        double roll = ThreadLocalRandom.current().nextDouble(100.0D);

        // 0.01%
        if (roll < 0.01D) {
            return new ItemStack(Material.NETHERITE_SCRAP, 1);
        }
        // 0.01% - 마황(마법이 부여된 황금사과)
        if (roll < 0.02D) {
            return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
        }
        // 1.0%
        if (roll < 1.02D) {
            return new ItemStack(Material.DIAMOND, 1);
        }
        // 1.0%
        if (roll < 2.02D) {
            return new ItemStack(Material.GOLDEN_APPLE, 2);
        }
        // 0.1%
        if (roll < 2.12D) {
            return new ItemStack(Material.SHULKER_BOX, 1);
        }
        return null;
    }
}
