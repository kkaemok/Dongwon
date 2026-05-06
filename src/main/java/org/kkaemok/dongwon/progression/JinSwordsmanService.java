package org.kkaemok.dongwon.progression;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

public final class JinSwordsmanService {
    private static final long COOLDOWN_MILLIS = 300_000L;
    private static final long DURATION_TICKS = 200L;

    private final Plugin plugin;
    private final MasteryService masteryService;
    private final NamespacedKey enhancedSwordKey;
    private final Map<UUID, ActiveState> activeStates = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public JinSwordsmanService(Plugin plugin, MasteryService masteryService) {
        this.plugin = plugin;
        this.masteryService = masteryService;
        this.enhancedSwordKey = new NamespacedKey(plugin, "jin_swordsman_enhanced");
    }

    public boolean tryActivate(Player player) {
        if (!masteryService.hasJinSwordsman(player)) {
            return false;
        }
        ItemStack current = player.getInventory().getItemInMainHand();
        if (!isSword(current)) {
            return false;
        }
        if (activeStates.containsKey(player.getUniqueId())) {
            player.sendMessage("진-검사 강화는 이미 활성화되어 있습니다.");
            return true;
        }

        long now = System.currentTimeMillis();
        long readyAt = cooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            long remain = (readyAt - now + 999L) / 1000L;
            player.sendMessage("진-검사 재사용 대기시간: " + remain + "초");
            return true;
        }

        ItemStack original = current.clone();
        ItemStack enhanced = createEnhancedSword();
        player.getInventory().setItemInMainHand(enhanced);
        player.updateInventory();

        cooldownUntil.put(player.getUniqueId(), now + COOLDOWN_MILLIS);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> deactivate(player.getUniqueId()), DURATION_TICKS);
        activeStates.put(player.getUniqueId(), new ActiveState(original, task));
        player.sendMessage("진-검사 강화 활성화: 10초 동안 강화된 칼 + 검기 발사");
        return true;
    }

    public void fireSwordWave(Player player) {
        if (!isActive(player)) {
            return;
        }
        if (!isEnhancedSword(player.getInventory().getItemInMainHand())) {
            return;
        }
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(player.getLocation().getDirection().normalize().multiply(3.2D));
        arrow.setDamage(10.0D);
        arrow.setCritical(true);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
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
        if (currentMain != null && !currentMain.getType().isAir() && !isEnhancedSword(currentMain)) {
            Map<Integer, ItemStack> leftover = inventory.addItem(currentMain.clone());
            if (!leftover.isEmpty()) {
                leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            }
        }
        inventory.setItemInMainHand(original);
        player.updateInventory();
        player.sendMessage("진-검사 강화 종료");
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
            drops.removeIf(this::isEnhancedSword);
            drops.add(state.originalMainHand.clone());
        } else {
            drops.removeIf(this::isEnhancedSword);
        }
        removeEnhancedSwords(player.getInventory());
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
