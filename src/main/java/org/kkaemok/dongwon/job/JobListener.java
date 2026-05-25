package org.kkaemok.dongwon.job;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.kkaemok.dongwon.progression.MasteryService;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class JobListener implements Listener {
    private static final Set<Material> AGEABLE_CROPS = Set.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.SWEET_BERRY_BUSH
    );
    private static final Set<Material> DIRECT_CROPS = Set.of(
            Material.MELON,
            Material.PUMPKIN
    );

    private final JobService jobService;
    private final MasteryService masteryService;

    public JobListener(JobService jobService, MasteryService masteryService) {
        this.jobService = jobService;
        this.masteryService = masteryService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) {
            return;
        }

        if (jobService.isImmuneToDebuff(player, newEffect.getType(), newEffect.getAmplifier())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        Entity caught = event.getCaught();
        if (!(caught instanceof Item itemEntity)) {
            return;
        }

        long fishingMasteryExp = masteryService.onFishing(player);
        ItemStack special = jobService.rollFishingLoot(player, fishingMasteryExp);
        if (special == null) {
            return;
        }
        itemEntity.setItemStack(special);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        JobService.AbilityUseResult result = jobService.handleAbilityItemUse(player, item);
        if (result == JobService.AbilityUseResult.NOT_ABILITY) {
            return;
        }

        event.setCancelled(true);
        if (result != JobService.AbilityUseResult.USED || item == null) {
            return;
        }

        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(amount - 1);
            player.getInventory().setItemInMainHand(item);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!jobService.isJob(player, JobType.FARMER)) {
            return;
        }

        Block block = event.getBlock();
        if (!isMatureCrop(block)) {
            return;
        }

        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
        for (ItemStack drop : drops) {
            int bonusAmount = calculateBonusAmount(drop.getAmount());
            if (bonusAmount <= 0) {
                continue;
            }
            ItemStack bonus = drop.clone();
            bonus.setAmount(bonusAmount);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), bonus);
        }
    }

    private boolean isMatureCrop(Block block) {
        Material type = block.getType();
        if (DIRECT_CROPS.contains(type)) {
            return true;
        }
        if (!AGEABLE_CROPS.contains(type) || !(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private int calculateBonusAmount(int baseAmount) {
        double exactBonus = baseAmount * 0.2D;
        int guaranteed = (int) exactBonus;
        double fractional = exactBonus - guaranteed;
        if (ThreadLocalRandom.current().nextDouble() < fractional) {
            guaranteed++;
        }
        return guaranteed;
    }
}
