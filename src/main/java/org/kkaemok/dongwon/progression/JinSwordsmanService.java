package org.kkaemok.dongwon.progression;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class JinSwordsmanService {
    private static final long COOLDOWN_MILLIS = 300_000L;
    private static final long DURATION_TICKS = 200L;
    private static final int SWORD_WAVE_ARROW_COUNT = 5;
    private static final double SWORD_WAVE_SPEED = 3.2D;
    private static final Particle.DustOptions SWORD_WAVE_DUST = new Particle.DustOptions(Color.RED, 1.2F);

    private final Plugin plugin;
    private final MasteryService masteryService;
    private final MasteryConfig config;
    private final NamespacedKey enhancedSwordKey;
    private final Map<UUID, ActiveState> activeStates = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public JinSwordsmanService(Plugin plugin, MasteryService masteryService, MasteryConfig config) {
        this.plugin = plugin;
        this.masteryService = masteryService;
        this.config = config;
        this.enhancedSwordKey = new NamespacedKey(plugin, "jin_swordsman_enhanced");
    }

    public void tryActivate(Player player) {
        if (!masteryService.hasJinSwordsman(player)) {
            return;
        }
        ItemStack current = player.getInventory().getItemInMainHand();
        if (!isSword(current)) {
            return;
        }
        if (activeStates.containsKey(player.getUniqueId())) {
            config.send(player, "jin-swordsman.already-active", "<red>진-검사 강화는 이미 활성화되어 있습니다.");
            return;
        }

        long now = System.currentTimeMillis();
        long readyAt = cooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            long remain = (readyAt - now + 999L) / 1000L;
            config.send(player, "jin-swordsman.cooldown", "<red>진-검사 재사용 대기시간: %seconds%초",
                    placeholder("seconds", remain));
            return;
        }

        ItemStack original = current.clone();
        ItemStack enhanced = createEnhancedSword();
        player.getInventory().setItemInMainHand(enhanced);
        player.updateInventory();

        cooldownUntil.put(player.getUniqueId(), now + COOLDOWN_MILLIS);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> deactivate(player.getUniqueId()), DURATION_TICKS);
        activeStates.put(player.getUniqueId(), new ActiveState(original, task));
        config.send(player, "jin-swordsman.activated", "<green>진-검사 강화 활성화: 10초 동안 강화된 칼 + 검기 발사");
    }

    public void fireSwordWave(Player player) {
        if (!isActive(player)) {
            return;
        }
        if (!isEnhancedSword(player.getInventory().getItemInMainHand())) {
            return;
        }
        for (int i = 0; i < SWORD_WAVE_ARROW_COUNT; i++) {
            Arrow arrow = player.launchProjectile(Arrow.class);
            arrow.setVelocity(player.getLocation().getDirection().normalize().multiply(SWORD_WAVE_SPEED));
            arrow.setDamage(10.0D);
            arrow.setCritical(true);
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            trackSwordWaveParticles(arrow);
        }
    }

    private void trackSwordWaveParticles(Arrow arrow) {
        BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (arrow.isDead() || arrow.isOnGround() || !arrow.isValid()) {
                taskRef[0].cancel();
                return;
            }
            Location location = arrow.getLocation();
            arrow.getWorld().spawnParticle(Particle.DUST, location, 8, 0.05D, 0.05D, 0.05D, 0.0D, SWORD_WAVE_DUST);
        }, 0L, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (taskRef[0] != null && !taskRef[0].isCancelled()) {
                taskRef[0].cancel();
            }
        }, 100L);
    }

    public boolean isEnhancedSword(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Boolean marked = meta.getPersistentDataContainer().get(enhancedSwordKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(marked);
    }

    public boolean isActive(Player player) {
        return activeStates.containsKey(player.getUniqueId());
    }

    public void deactivate(UUID playerId) {
        ActiveState state = activeStates.remove(playerId);
        if (state == null) {
            return;
        }
        state.task.cancel();

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        removeEnhancedSwords(inventory);
        ItemStack original = state.originalMainHand.clone();
        ItemStack currentMain = inventory.getItemInMainHand();
        if (!currentMain.getType().isAir() && !isEnhancedSword(currentMain)) {
            Map<Integer, ItemStack> leftover = inventory.addItem(currentMain.clone());
            if (!leftover.isEmpty()) {
                leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            }
        }
        inventory.setItemInMainHand(original);
        player.updateInventory();
        config.send(player, "jin-swordsman.ended", "<yellow>진-검사 강화 종료");
    }

    public void handleQuit(Player player) {
        UUID playerId = player.getUniqueId();
        ActiveState state = activeStates.remove(playerId);
        if (state != null) {
            state.task.cancel();
            player.getInventory().setItemInMainHand(state.originalMainHand.clone());
            player.updateInventory();
        }
        removeEnhancedSwords(player.getInventory());
    }

    public void handleDeath(Player player, List<ItemStack> drops) {
        UUID playerId = player.getUniqueId();
        ActiveState state = activeStates.remove(playerId);
        if (state != null) {
            state.task.cancel();
            removeEnhancedSwordDrops(drops);
            drops.add(state.originalMainHand.clone());
        } else {
            removeEnhancedSwordDrops(drops);
        }
        removeEnhancedSwords(player.getInventory());
    }

    private void removeEnhancedSwordDrops(List<ItemStack> drops) {
        drops.removeIf(this::isEnhancedSword);
    }

    private void removeEnhancedSwords(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (isEnhancedSword(stack)) {
                contents[i] = null;
            }
        }
        inventory.setContents(contents);
    }

    private ItemStack createEnhancedSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.displayName(Component.text("강화된 진-검사 검"));
        meta.addEnchant(Enchantment.SHARPNESS, 7, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(enhancedSwordKey, PersistentDataType.BOOLEAN, true);
        sword.setItemMeta(meta);
        return sword;
    }

    private boolean isSword(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getType().name().toLowerCase(Locale.ROOT).endsWith("_sword");
    }

    private record ActiveState(ItemStack originalMainHand, BukkitTask task) {
    }
}
