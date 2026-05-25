package org.kkaemok.dongwon.progression;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ShihyeonryuService {
    private static final int PRIMED_STRENGTH_LEVEL = 50;
    private static final int BURNOUT_START_STRENGTH_LEVEL = 30;
    private static final int BURNOUT_END_STRENGTH_LEVEL = 1;
    private static final long BURNOUT_DURATION_TICKS = 200L;
    private static final int ROOT_LEVEL = 5;

    private final Plugin plugin;
    private final MasteryService masteryService;
    private final MasteryConfig config;
    private final Set<UUID> primedPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> burnoutTasks = new HashMap<>();

    public ShihyeonryuService(Plugin plugin, MasteryService masteryService, MasteryConfig config) {
        this.plugin = plugin;
        this.masteryService = masteryService;
        this.config = config;
    }

    public void tryPrime(Player player) {
        if (!masteryService.hasSpecialization(player, MasterySpecialization.SHIHYEONRYU)) {
            return;
        }
        if (!isSword(player.getInventory().getItemInMainHand())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (burnoutTasks.containsKey(playerId)) {
            config.send(player, "shihyeonryu.burnout-running", "<red>시현류 후딜이 진행 중입니다.");
            return;
        }
        if (primedPlayers.contains(playerId)) {
            config.send(player, "shihyeonryu.already-primed", "<red>시현류는 이미 예열 상태입니다.");
            return;
        }

        primedPlayers.add(playerId);
        applyPotionEffect(player, new PotionEffect(
                PotionEffectType.STRENGTH,
                Integer.MAX_VALUE,
                PRIMED_STRENGTH_LEVEL - 1,
                false,
                false,
                true
        ));
        config.send(player, "shihyeonryu.primed", "<green>시현류 예열 완료: 다음 검 타격 1회에 올인합니다.");
    }

    public void onMeleeHit(Player player) {
        UUID playerId = player.getUniqueId();
        if (!primedPlayers.remove(playerId)) {
            return;
        }

        player.removePotionEffect(PotionEffectType.STRENGTH);
        startBurnout(player);
    }

    public void handleQuit(Player player) {
        clearState(player);
    }

    public void handleDeath(Player player) {
        clearState(player);
    }

    private void clearState(Player player) {
        UUID playerId = player.getUniqueId();
        primedPlayers.remove(playerId);
        BukkitTask task = burnoutTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private void startBurnout(Player player) {
        UUID playerId = player.getUniqueId();
        if (burnoutTasks.containsKey(playerId)) {
            return;
        }

        applyPotionEffect(player, new PotionEffect(
                PotionEffectType.SLOWNESS,
                (int) BURNOUT_DURATION_TICKS,
                ROOT_LEVEL - 1,
                false,
                false,
                true
        ));
        config.send(player, "shihyeonryu.burnout-started", "<yellow>시현류 반동 시작: 10초간 힘이 30에서 1로 감소합니다.");

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long elapsed = 0L;

            @Override
            public void run() {
                Player online = Bukkit.getPlayer(playerId);
                if (online == null || online.isDead()) {
                    stop();
                    return;
                }

                if (elapsed >= BURNOUT_DURATION_TICKS) {
                    online.removePotionEffect(PotionEffectType.STRENGTH);
                    online.removePotionEffect(PotionEffectType.SLOWNESS);
                    config.send(online, "shihyeonryu.burnout-ended", "<yellow>시현류 반동 종료");
                    stop();
                    return;
                }

                int level = calculateStrengthLevel(elapsed);
                applyPotionEffect(online, new PotionEffect(
                        PotionEffectType.STRENGTH,
                        12,
                        level - 1,
                        false,
                        false,
                        true
                ));
                elapsed += 10L;
            }

            private void stop() {
                BukkitTask running = burnoutTasks.remove(playerId);
                if (running != null) {
                    running.cancel();
                }
            }
        }, 0L, 10L);

        burnoutTasks.put(playerId, task);
    }

    private int calculateStrengthLevel(long elapsedTicks) {
        double progress = Math.clamp(elapsedTicks / (double) BURNOUT_DURATION_TICKS, 0.0D, 1.0D);
        int delta = BURNOUT_START_STRENGTH_LEVEL - BURNOUT_END_STRENGTH_LEVEL;
        int level = BURNOUT_START_STRENGTH_LEVEL - (int) Math.floor(delta * progress);
        return Math.clamp(level, BURNOUT_END_STRENGTH_LEVEL, BURNOUT_START_STRENGTH_LEVEL);
    }

    private void applyPotionEffect(Player player, PotionEffect effect) {
        player.addPotionEffect(effect);
    }

    private boolean isSword(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getType().name().toLowerCase(Locale.ROOT).endsWith("_sword");
    }
}
