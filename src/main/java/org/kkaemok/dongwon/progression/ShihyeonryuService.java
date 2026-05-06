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
    private final Set<UUID> primedPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> burnoutTasks = new HashMap<>();

    public ShihyeonryuService(Plugin plugin, MasteryService masteryService) {
        this.plugin = plugin;
        this.masteryService = masteryService;
    }

    public boolean tryPrime(Player player) {
        if (!masteryService.hasSpecialization(player, MasterySpecialization.SHIHYEONRYU)) {
            return false;
        }
        if (!isSword(player.getInventory().getItemInMainHand())) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (burnoutTasks.containsKey(playerId)) {
            player.sendMessage("시현류 후딜이 진행 중입니다.");
            return true;
        }
        if (primedPlayers.contains(playerId)) {
            player.sendMessage("시현류는 이미 예열 상태입니다.");
            return true;
        }

        primedPlayers.add(playerId);
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, PRIMED_STRENGTH_LEVEL - 1, false, false, true),
                true
        );
        player.sendMessage("시현류 예열 완료: 다음 검 타격 1회에 올인합니다.");
        return true;
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

        player.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, (int) BURNOUT_DURATION_TICKS, ROOT_LEVEL - 1, false, false, true),
                true
        );
        player.sendMessage("시현류 반동 시작: 10초간 힘이 30에서 1로 감소합니다.");

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
                    online.sendMessage("시현류 반동 종료");
                    stop();
                    return;
                }

                int level = calculateStrengthLevel(elapsed);
                online.addPotionEffect(
                        new PotionEffect(PotionEffectType.STRENGTH, 12, level - 1, false, false, true),
                        true
                );
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
        double progress = Math.min(1.0D, Math.max(0.0D, elapsedTicks / (double) BURNOUT_DURATION_TICKS));
        int delta = BURNOUT_START_STRENGTH_LEVEL - BURNOUT_END_STRENGTH_LEVEL;
        int level = BURNOUT_START_STRENGTH_LEVEL - (int) Math.floor(delta * progress);
        return Math.max(BURNOUT_END_STRENGTH_LEVEL, level);
    }

    private boolean isSword(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getType().name().toLowerCase(Locale.ROOT).endsWith("_sword");
    }
}
