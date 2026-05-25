package org.kkaemok.dongwon.job.jobs;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.kkaemok.dongwon.job.JobType;
import org.kkaemok.dongwon.progression.FishermanMasteryLevel;

import java.util.concurrent.ThreadLocalRandom;

public final class FishermanJob implements JobDefinition {
    @Override
    public JobType getType() {
        return JobType.FISHERMAN;
    }

    @Override
    public @Nullable ItemStack rollFishingLoot(long fishingMasteryExp) {
        FishermanMasteryLevel level = FishermanMasteryLevel.resolveByExp(fishingMasteryExp);
        double roll = ThreadLocalRandom.current().nextDouble(100.0D);
        double cumulative = 0.0D;

        for (LootEntry entry : lootTable(level)) {
            cumulative += entry.chancePercent();
            if (roll < cumulative) {
                return new ItemStack(entry.material(), entry.amount());
            }
        }
        return null;
    }

    private LootEntry[] lootTable(FishermanMasteryLevel level) {
        return switch (level) {
            case LEVEL_3 -> new LootEntry[]{
                    new LootEntry(Material.DIAMOND, 10, 10.0D),
                    new LootEntry(Material.NETHERITE_SCRAP, 1, 1.0D),
                    new LootEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 1.0D),
                    new LootEntry(Material.GOLDEN_APPLE, 5, 2.0D),
                    new LootEntry(Material.SHULKER_BOX, 1, 2.0D)
            };
            case LEVEL_2 -> new LootEntry[]{
                    new LootEntry(Material.DIAMOND, 10, 1.0D),
                    new LootEntry(Material.NETHERITE_SCRAP, 1, 0.5D),
                    new LootEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 0.5D),
                    new LootEntry(Material.GOLDEN_APPLE, 5, 1.0D),
                    new LootEntry(Material.SHULKER_BOX, 1, 1.0D)
            };
            case LEVEL_1 -> new LootEntry[]{
                    new LootEntry(Material.DIAMOND, 2, 1.0D),
                    new LootEntry(Material.NETHERITE_SCRAP, 1, 0.1D),
                    new LootEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 0.1D),
                    new LootEntry(Material.GOLDEN_APPLE, 3, 1.0D),
                    new LootEntry(Material.SHULKER_BOX, 1, 0.1D)
            };
            case LEVEL_0 -> new LootEntry[]{
                    new LootEntry(Material.NETHERITE_SCRAP, 1, 0.01D),
                    new LootEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 0.01D),
                    new LootEntry(Material.DIAMOND, 1, 1.0D),
                    new LootEntry(Material.GOLDEN_APPLE, 2, 1.0D),
                    new LootEntry(Material.SHULKER_BOX, 1, 0.1D)
            };
        };
    }

    private record LootEntry(Material material, int amount, double chancePercent) {
    }
}
