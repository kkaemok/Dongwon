package org.kkaemok.dongwon.job;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.kkaemok.dongwon.job.jobs.JobDefinition;
import org.kkaemok.dongwon.party.PartyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JobService {
    private static final int EFFECT_DURATION_TICKS = 80;
    private static final int AURA_EFFECT_DURATION_TICKS = 60;
    private static final int HEALER_PASSIVE_DURATION_TICKS = 20;
    private static final int HEALER_PASSIVE_INTERVAL_MILLIS = 5_000;
    private static final int ABILITY_ITEM_INTERVAL_MILLIS = 120_000;
    private static final String HOLY_ARMOR_KEY = "holy_armor";
    private static final String HEAL_KEY = "heal";

    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final PartyManager partyManager;
    private final JobRegistry jobRegistry;
    private final Set<PotionEffectType> managedPassiveEffects;
    private final NamespacedKey abilityItemKey;
    private final Map<UUID, Double> baseHealthByPlayer = new HashMap<>();
    private final Map<UUID, Long> nextHealerPulseAt = new HashMap<>();
    private final Map<UUID, Long> nextAbilityItemAt = new HashMap<>();
    private final Map<UUID, JobType> abilityGrantJobByPlayer = new HashMap<>();
    private BukkitTask passiveTask;

    public JobService(JavaPlugin plugin, JobManager jobManager, PartyManager partyManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.partyManager = partyManager;
        this.jobRegistry = new JobRegistry();
        this.managedPassiveEffects = jobRegistry.getManagedPassiveEffects();
        this.abilityItemKey = new NamespacedKey(plugin, "job_ability");
    }

    public void start() {
        if (passiveTask != null) {
            passiveTask.cancel();
        }
        passiveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyJobPassives(player, now);
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
        applyJobPassives(player, System.currentTimeMillis());
    }

    private void applyJobPassives(Player player, long now) {
        JobDefinition definition = getCurrentJob(player);
        applyHealthBonus(player, definition);
        applyPotionPassives(player, definition);
        clearImmuneDebuffs(player, definition);
        applySpecialJobPassives(player, now);
        grantAbilityItemIfReady(player, now);
    }

    public void applyJobPassivesImmediately(Player player) {
        clearManagedPassiveEffects(player);
        applyJobPassives(player);
    }

    public boolean isImmuneToDebuff(Player player, PotionEffectType type, int amplifier) {
        int requestedLevel = amplifier + 1;
        return getCurrentJob(player).isDebuffImmune(type, requestedLevel);
    }

    public @Nullable ItemStack rollFishingLoot(Player player, long fishingMasteryExp) {
        return getCurrentJob(player).rollFishingLoot(fishingMasteryExp);
    }

    public boolean isJob(Player player, JobType jobType) {
        return jobManager.getJob(player.getUniqueId()) == jobType;
    }

    public AbilityUseResult handleAbilityItemUse(Player player, ItemStack itemStack) {
        String ability = abilityKey(itemStack);
        if (ability == null) {
            return AbilityUseResult.NOT_ABILITY;
        }

        JobType jobType = jobManager.getJob(player.getUniqueId());
        if (HOLY_ARMOR_KEY.equals(ability)) {
            if (jobType != JobType.BUFFER) {
                player.sendMessage(Component.text("버퍼 직업만 홀리 아머를 사용할 수 있습니다."));
                return AbilityUseResult.DENIED;
            }
            for (Player target : nearbyPartyMembers(player, 10.0D)) {
                addOrRefreshEffect(target, PotionEffectType.RESISTANCE, 1, 10 * 20);
            }
            player.sendMessage(Component.text("홀리 아머를 사용했습니다."));
            return AbilityUseResult.USED;
        }

        if (HEAL_KEY.equals(ability)) {
            if (jobType != JobType.HEALER) {
                player.sendMessage(Component.text("힐러 직업만 힐을 사용할 수 있습니다."));
                return AbilityUseResult.DENIED;
            }
            for (Player target : nearbyPartyMembers(player, 20.0D)) {
                addOrRefreshEffect(target, PotionEffectType.REGENERATION, 5, 5 * 20);
            }
            player.sendMessage(Component.text("힐을 사용했습니다."));
            return AbilityUseResult.USED;
        }

        return AbilityUseResult.NOT_ABILITY;
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
            player.addPotionEffect(target);
        }
    }

    private void applySpecialJobPassives(Player player, long now) {
        JobType jobType = jobManager.getJob(player.getUniqueId());
        if (jobType == JobType.BUFFER) {
            nextHealerPulseAt.remove(player.getUniqueId());
            for (Player target : nearbyPlayers(player, 10.0D, true)) {
                addOrRefreshEffect(target, PotionEffectType.STRENGTH, 1, AURA_EFFECT_DURATION_TICKS);
                addOrRefreshEffect(target, PotionEffectType.SPEED, 1, AURA_EFFECT_DURATION_TICKS);
            }
            return;
        }

        if (jobType == JobType.HEALER) {
            UUID playerId = player.getUniqueId();
            long nextPulse = nextHealerPulseAt.getOrDefault(playerId, 0L);
            if (now >= nextPulse) {
                for (Player target : nearbyPartyMembers(player, 20.0D)) {
                    addOrRefreshEffect(target, PotionEffectType.REGENERATION, 1, HEALER_PASSIVE_DURATION_TICKS);
                }
                nextHealerPulseAt.put(playerId, now + HEALER_PASSIVE_INTERVAL_MILLIS);
            }
            return;
        }

        nextHealerPulseAt.remove(player.getUniqueId());
    }

    private void grantAbilityItemIfReady(Player player, long now) {
        JobType jobType = jobManager.getJob(player.getUniqueId());
        if (jobType != JobType.BUFFER && jobType != JobType.HEALER) {
            nextAbilityItemAt.remove(player.getUniqueId());
            abilityGrantJobByPlayer.remove(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();
        JobType previousGrantJob = abilityGrantJobByPlayer.get(playerId);
        if (previousGrantJob != jobType) {
            abilityGrantJobByPlayer.put(playerId, jobType);
            nextAbilityItemAt.put(playerId, now + ABILITY_ITEM_INTERVAL_MILLIS);
            return;
        }

        long nextGrant = nextAbilityItemAt.getOrDefault(playerId, now + ABILITY_ITEM_INTERVAL_MILLIS);
        if (now < nextGrant) {
            nextAbilityItemAt.putIfAbsent(playerId, nextGrant);
            return;
        }

        if (jobType != JobType.BUFFER || !hasAbilityItem(player, HOLY_ARMOR_KEY)) {
            giveAbilityItem(player, jobType);
        }
        nextAbilityItemAt.put(playerId, now + ABILITY_ITEM_INTERVAL_MILLIS);
    }

    private void giveAbilityItem(Player player, JobType jobType) {
        ItemStack item = jobType == JobType.BUFFER ? createHolyArmorItem() : createHealItem();
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        if (jobType == JobType.BUFFER) {
            player.sendMessage(Component.text("홀리 아머를 획득했습니다."));
        } else {
            player.sendMessage(Component.text("힐을 획득했습니다."));
        }
    }

    private ItemStack createHolyArmorItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text("홀리 아머"));
        meta.lore(java.util.List.of(
                Component.text("주변 파티원에게 저항 I을 10초간 제공합니다."),
                Component.text("버퍼 전용")
        ));
        meta.getPersistentDataContainer().set(abilityItemKey, PersistentDataType.STRING, HOLY_ARMOR_KEY);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHealItem() {
        ItemStack item = new ItemStack(Material.GLISTERING_MELON_SLICE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text("힐"));
        meta.lore(java.util.List.of(
                Component.text("주변 파티원에게 재생 V를 5초간 제공합니다."),
                Component.text("힐러 전용")
        ));
        meta.getPersistentDataContainer().set(abilityItemKey, PersistentDataType.STRING, HEAL_KEY);
        item.setItemMeta(meta);
        return item;
    }

    private String abilityKey(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().get(abilityItemKey, PersistentDataType.STRING);
    }

    private boolean hasAbilityItem(Player player, String ability) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (ability.equals(abilityKey(itemStack))) {
                return true;
            }
        }
        return ability.equals(abilityKey(player.getInventory().getItemInOffHand()));
    }

    private java.util.List<Player> nearbyPlayers(Player source, double radius, boolean includeSelf) {
        double radiusSquared = radius * radius;
        return Bukkit.getOnlinePlayers().stream()
                .map(Player.class::cast)
                .filter(target -> includeSelf || !target.getUniqueId().equals(source.getUniqueId()))
                .filter(target -> target.getWorld().equals(source.getWorld()))
                .filter(target -> target.getLocation().distanceSquared(source.getLocation()) <= radiusSquared)
                .toList();
    }

    private java.util.List<Player> nearbyPartyMembers(Player source, double radius) {
        double radiusSquared = radius * radius;
        return partyManager.getOnlineMembersExcept(source).stream()
                .filter(target -> target.getWorld().equals(source.getWorld()))
                .filter(target -> target.getLocation().distanceSquared(source.getLocation()) <= radiusSquared)
                .toList();
    }

    private void addOrRefreshEffect(Player target, PotionEffectType type, int level, int durationTicks) {
        int amplifier = level - 1;
        PotionEffect existing = target.getPotionEffect(type);
        if (existing != null) {
            if (existing.getAmplifier() > amplifier) {
                return;
            }
            if (existing.getAmplifier() == amplifier && existing.getDuration() > durationTicks) {
                return;
            }
        }
        target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false, true));
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

    public enum AbilityUseResult {
        NOT_ABILITY,
        DENIED,
        USED
    }
}
