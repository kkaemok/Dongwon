package org.kkaemok.dongwon.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.kkaemok.dongwon.job.JobService;

public final class JobListener implements Listener {
    private final JobService jobService;

    public JobListener(JobService jobService) {
        this.jobService = jobService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null || newEffect.getType() == null) {
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

        ItemStack special = jobService.rollFishingLoot(player);
        if (special == null) {
            return;
        }
        itemEntity.setItemStack(special);
    }
}
