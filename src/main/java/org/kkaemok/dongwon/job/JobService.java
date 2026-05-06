package org.kkaemok.dongwon.job;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.kkaemok.dongwon.job.jobs.JobDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JobService {
    private static final int EFFECT_DURATION_TICKS = 80;

    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final JobRegistry jobRegistry;
    private final Set<PotionEffectType> managedPassiveEffects;
    private final Map<UUID, Double> baseHealthByPlayer = new HashMap<>();
    private BukkitTask passiveTask;

    public JobService(JavaPlugin plugin, JobManager jobManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.jobRegistry = new JobRegistry();
        this.managedPassiveEffects = jobRegistry.getManagedPassiveEffects();
    }

    public void start() {
        if (passiveTask != null) {
            passiveTask.cancel();
        }
        passiveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyJobPassives(player);
            }
        }, 1L, 20L);
    }

    public void stop() {
        if (passiveTask != null) {
            passiveTask.cancel();
            passiveTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearManagedHealth(player);
        }
    }

    public void applyJobPassives(Player player) {
        JobDefinition definition = getCurrentJob(player);
        applyHealthBonus(player, definition);
        applyPotionPassives(player, definition);
        clearImmuneDebuffs(player, definition);
    }

    public void applyJobPassivesImmediately(Player player) {
        clearManagedPassiveEffects(player);
        applyJobPassives(player);
    }

    public boolean isImmuneToDebuff(Player player, PotionEffectType type, int amplifier) {
        int requestedLevel = amplifier + 1;
        return getCurrentJob(player).isDebuffImmune(type, requestedLevel);
    }

    public @Nullable ItemStack rollFishingLoot(Player player) {
        return getCurrentJob(player).rollFishingLoot();
    }

    private void applyPotionPassives(Player player, JobDefinition definition) {
        Map<PotionEffectType, Integer> desired = definition.getPassiveEffects(player);
        // This plugin only removes passive effects it is responsible for.
        for (PotionEffectType type : managedPassiveEffects) {
            if (!desired.containsKey(type)) {
                PotionEffect existing = player.getPotionEffect(type);
                if (existing != null && existing.isAmbient() && !existing.hasParticles()) {
                    player.removePotionEffect(type);
                }
            }
        }

        for (Map.Entry<PotionEffectType, Integer> entry : desired.entrySet()) {
            PotionEffectType type = entry.getKey();
            int level = entry.getValue();
            PotionEffect target = new PotionEffect(type, EFFECT_DURATION_TICKS, level - 1, true, false, true);
            player.addPotionEffect(target, true);
        }
    }

    private void clearManagedPassiveEffects(Player player) {
        for (PotionEffectType type : managedPassiveEffects) {
            PotionEffect existing = player.getPotionEffect(type);
            if (existing != null && existing.isAmbient() && !existing.hasParticles()) {
                player.removePotionEffect(type);
            }
        }
    }

    private void applyHealthBonus(Player player, JobDefinition definition) {
        AttributeInstance attribute = resolveMaxHealthAttribute(player);
        if (attribute == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        baseHealthByPlayer.putIfAbsent(uuid, attribute.getBaseValue());
        double base = baseHealthByPlayer.getOrDefault(uuid, attribute.getBaseValue());
        double target = base + definition.getMaxHealthBonus();
        if (Math.abs(attribute.getBaseValue() - target) > 0.01D) {
            attribute.setBaseValue(target);
            if (player.getHealth() > target) {
                player.setHealth(target);
            }
        }
    }

    private void clearImmuneDebuffs(Player player, JobDefinition definition) {
        for (PotionEffect activeEffect : player.getActivePotionEffects()) {
            PotionEffectType type = activeEffect.getType();
            if (type == null) {
                continue;
            }
            int level = activeEffect.getAmplifier() + 1;
            if (definition.isDebuffImmune(type, level)) {
                player.removePotionEffect(type);
            }
        }
    }

    public void clearManagedHealth(Player player) {
        AttributeInstance attribute = resolveMaxHealthAttribute(player);
        if (attribute == null) {
            return;
        }
        Double base = baseHealthByPlayer.get(player.getUniqueId());
        if (base != null && Math.abs(attribute.getBaseValue() - base) > 0.01D) {
            attribute.setBaseValue(base);
            if (player.getHealth() > base) {
                player.setHealth(base);
            }
        }
    }

    private static AttributeInstance resolveMaxHealthAttribute(Player player) {
        return player.getAttribute(Attribute.MAX_HEALTH);
    }

    private JobDefinition getCurrentJob(Player player) {
        JobType jobType = jobManager.getJob(player.getUniqueId());
        return jobRegistry.get(jobType);
    }
}
