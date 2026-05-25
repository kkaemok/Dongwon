package org.kkaemok.dongwon.tpa;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TpaMenu {
    public static final int CANCEL_SLOT = 10;
    public static final int LOCATION_SLOT = 12;
    public static final int PLAYER_SLOT = 13;
    public static final int REGION_SLOT = 14;
    public static final int CONFIRM_SLOT = 16;

    private final Plugin plugin;
    private final TpaConfig config;
    private final Set<UUID> activeViewers = new HashSet<>();
    private BukkitTask refreshTask;

    public TpaMenu(Plugin plugin, TpaConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void open(Player sender, Player target) {
        Holder holder = new Holder(sender.getUniqueId(), sender.getUniqueId(), target.getUniqueId(), target.getName(), Mode.REQUEST_CONFIRM);
        Inventory inventory = Bukkit.createInventory(holder, 27, config.menuTitle());
        holder.bind(inventory);
        render(inventory, holder);
        sender.openInventory(inventory);
        track(sender.getUniqueId());
    }

    public void openReply(Player target, Player requester) {
        Holder holder = new Holder(target.getUniqueId(), requester.getUniqueId(), target.getUniqueId(), requester.getName(), Mode.REQUEST_REPLY);
        Inventory inventory = Bukkit.createInventory(holder, 27, config.replyMenuTitle());
        holder.bind(inventory);
        render(inventory, holder);
        target.openInventory(inventory);
        track(target.getUniqueId());
    }

    public boolean isMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof Holder;
    }

    public Context context(Inventory inventory) {
        if (!(inventory.getHolder() instanceof Holder holder)) {
            return null;
        }
        return new Context(holder.viewerId(), holder.requesterId(), holder.targetId(), holder.mode());
    }

    public void onClose(Player player) {
        untrack(player.getUniqueId());
    }

    public void untrack(UUID playerId) {
        if (!activeViewers.remove(playerId)) {
            return;
        }
        stopRefreshIfIdle();
    }

    public void shutdown() {
        activeViewers.clear();
        stopRefreshTask();
    }

    public void reload() {
        stopRefreshTask();
        if (!activeViewers.isEmpty()) {
            ensureRefreshTask();
        }
    }

    private void track(UUID playerId) {
        if (!activeViewers.add(playerId)) {
            return;
        }
        ensureRefreshTask();
    }

    private void ensureRefreshTask() {
        if (refreshTask != null) {
            return;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshActiveMenus,
                config.menuRefreshIntervalTicks(), config.menuRefreshIntervalTicks());
    }

    private void refreshActiveMenus() {
        if (activeViewers.isEmpty()) {
            stopRefreshTask();
            return;
        }

        Iterator<UUID> iterator = activeViewers.iterator();
        while (iterator.hasNext()) {
            UUID viewerId = iterator.next();
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                iterator.remove();
                continue;
            }

            Inventory top = viewer.getOpenInventory().getTopInventory();
            if (!(top.getHolder() instanceof Holder holder)) {
                iterator.remove();
                continue;
            }

            render(top, holder);
        }
        stopRefreshIfIdle();
    }

    private void stopRefreshIfIdle() {
        if (activeViewers.isEmpty()) {
            stopRefreshTask();
        }
    }

    private void stopRefreshTask() {
        if (refreshTask == null) {
            return;
        }
        refreshTask.cancel();
        refreshTask = null;
    }

    private void render(Inventory inventory, Holder holder) {
        Player viewed = viewedPlayer(holder);
        String viewedName = viewed == null ? holder.otherName() : viewed.getName();

        inventory.setItem(CANCEL_SLOT, buildCancelItem(holder, viewedName));
        inventory.setItem(11, null);
        inventory.setItem(LOCATION_SLOT, buildLocationItem(holder, viewed, viewedName));
        inventory.setItem(PLAYER_SLOT, buildPlayerItem(holder, viewed, viewedName));
        inventory.setItem(REGION_SLOT, buildRegionItem(holder, viewed, viewedName));
        inventory.setItem(15, null);
        inventory.setItem(CONFIRM_SLOT, buildConfirmItem(holder, viewedName));
    }

    private ItemStack buildCancelItem(Holder holder, String viewedName) {
        boolean reply = holder.mode() == Mode.REQUEST_REPLY;
        ItemStack item = new ItemStack(reply
                ? config.replyMenuMaterial("deny", Material.RED_STAINED_GLASS_PANE)
                : config.menuMaterial("cancel", Material.RED_STAINED_GLASS_PANE));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        Map<String, String> placeholders = placeholders(holder, viewedName, null);
        if (reply) {
            meta.displayName(config.replyMenuName("deny", "<red>거절", placeholders));
            meta.lore(config.replyMenuLore("deny", List.of("<white>%requester% 님의 TPA 요청을 거절합니다."), placeholders));
        } else {
            meta.displayName(config.menuName("cancel", "<red>취소", placeholders));
            meta.lore(config.menuLore("cancel", List.of("<white>TPA 요청을 보내지 않습니다."), placeholders));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLocationItem(Holder holder, Player viewed, String viewedName) {
        boolean reply = holder.mode() == Mode.REQUEST_REPLY;
        ItemStack item = new ItemStack(reply
                ? config.replyMenuMaterial("location", Material.GRASS_BLOCK)
                : config.menuMaterial("location", Material.GRASS_BLOCK));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String worldLabel = viewed == null ? "Unknown" : toWorldLabel(viewed.getWorld());
        Map<String, String> placeholders = placeholders(holder, viewedName, worldLabel);
        if (reply) {
            meta.displayName(config.replyMenuName("location", "<green>요청자 위치", placeholders));
            meta.lore(config.replyMenuLore("location", List.of("<white>%world%"), placeholders));
        } else {
            meta.displayName(config.menuName("location", "<green>위치", placeholders));
            meta.lore(config.menuLore("location", List.of("<white>%world%"), placeholders));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPlayerItem(Holder holder, Player viewed, String viewedName) {
        boolean reply = holder.mode() == Mode.REQUEST_REPLY;
        ItemStack item = new ItemStack(reply
                ? config.replyMenuMaterial("player", Material.PLAYER_HEAD)
                : config.menuMaterial("player", Material.PLAYER_HEAD));
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta skullMeta)) {
            return item;
        }

        if (viewed != null) {
            skullMeta.setOwningPlayer(viewed);
        }
        Map<String, String> placeholders = placeholders(holder, viewedName, null);
        if (reply) {
            skullMeta.displayName(config.replyMenuName("player", "<green>요청자", placeholders));
            skullMeta.lore(config.replyMenuLore("player", List.of("<white>%requester%"), placeholders));
        } else {
            skullMeta.displayName(config.menuName("player", "<green>플레이어", placeholders));
            skullMeta.lore(config.menuLore("player", List.of("<white>%target%"), placeholders));
        }
        item.setItemMeta(skullMeta);
        return item;
    }

    private ItemStack buildRegionItem(Holder holder, Player viewed, String viewedName) {
        boolean reply = holder.mode() == Mode.REQUEST_REPLY;
        ItemStack item = new ItemStack(reply
                ? config.replyMenuMaterial("region", Material.FEATHER)
                : config.menuMaterial("region", Material.FEATHER));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Map<String, String> placeholders = placeholders(holder, viewedName, null);
        placeholders = new java.util.HashMap<>(placeholders);
        placeholders.put("region", config.regionLabel());
        placeholders.put("ping", Integer.toString(viewed == null ? 0 : viewed.getPing()));
        if (reply) {
            meta.displayName(config.replyMenuName("region", "<green>지역", placeholders));
            meta.lore(config.replyMenuLore("region", List.of("<white>%region% <aqua>%ping%ms"), placeholders));
        } else {
            meta.displayName(config.menuName("region", "<green>지역", placeholders));
            meta.lore(config.menuLore("region", List.of("<white>%region% <aqua>%ping%ms"), placeholders));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmItem(Holder holder, String viewedName) {
        boolean reply = holder.mode() == Mode.REQUEST_REPLY;
        ItemStack item = new ItemStack(reply
                ? config.replyMenuMaterial("accept", Material.LIME_STAINED_GLASS_PANE)
                : config.menuMaterial("confirm", Material.LIME_STAINED_GLASS_PANE));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Map<String, String> placeholders = placeholders(holder, viewedName, null);
        if (reply) {
            meta.displayName(config.replyMenuName("accept", "<green>수락", placeholders));
            meta.lore(config.replyMenuLore("accept", List.of("<white>%requester% 님의 TPA 요청을 수락합니다."), placeholders));
        } else {
            meta.displayName(config.menuName("confirm", "<green>확인", placeholders));
            meta.lore(config.menuLore("confirm", List.of("<white>%target% 님에게 TPA 요청을 보냅니다."), placeholders));
        }
        item.setItemMeta(meta);
        return item;
    }

    private Player viewedPlayer(Holder holder) {
        UUID viewedId = holder.mode() == Mode.REQUEST_REPLY ? holder.requesterId() : holder.targetId();
        return Bukkit.getPlayer(viewedId);
    }

    private Map<String, String> placeholders(Holder holder, String viewedName, String worldLabel) {
        Player target = Bukkit.getPlayer(holder.targetId());
        String targetName = target == null ? holder.targetId().toString() : target.getName();
        String requesterName = holder.mode() == Mode.REQUEST_REPLY ? viewedName : holder.viewerId().toString();
        Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("target", holder.mode() == Mode.REQUEST_REPLY ? targetName : viewedName);
        placeholders.put("requester", requesterName);
        placeholders.put("world", worldLabel == null ? "Unknown" : worldLabel);
        return placeholders;
    }

    private String toWorldLabel(World world) {
        return switch (world.getEnvironment()) {
            case NORMAL -> "Overworld";
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> world.getName();
        };
    }

    private static final class Holder implements InventoryHolder {
        private final UUID viewerId;
        private final UUID requesterId;
        private final UUID targetId;
        private final String otherName;
        private final Mode mode;
        private Inventory inventory;

        private Holder(UUID viewerId, UUID requesterId, UUID targetId, String otherName, Mode mode) {
            this.viewerId = viewerId;
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.otherName = otherName;
            this.mode = mode;
        }

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        private UUID viewerId() {
            return viewerId;
        }

        private UUID requesterId() {
            return requesterId;
        }

        private UUID targetId() {
            return targetId;
        }

        private String otherName() {
            return otherName;
        }

        private Mode mode() {
            return mode;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public enum Mode {
        REQUEST_CONFIRM,
        REQUEST_REPLY
    }

    public record Context(UUID viewerId, UUID requesterId, UUID targetId, Mode mode) {
    }
}
