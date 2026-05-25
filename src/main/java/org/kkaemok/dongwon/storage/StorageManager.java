package org.kkaemok.dongwon.storage;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StorageManager {
    private final JavaPlugin plugin;
    private final StorageConfig config;
    private final File file;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private YamlConfiguration data;

    public StorageManager(JavaPlugin plugin, StorageConfig config) {
        this.plugin = plugin;
        this.config = config;
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "storagedata.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void open(Player player) {
        saveOpenInventory(player.getUniqueId());

        StorageHolder holder = new StorageHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(
                holder,
                config.size(),
                config.title(player)
        );
        holder.bind(inventory);
        inventory.setContents(loadContents(player.getUniqueId(), inventory.getSize()));
        openInventories.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
        config.playSound(player, "open");
        config.send(player, "opened", "");
    }

    public boolean isStorage(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof StorageHolder;
    }

    public void handleClose(Player player, Inventory inventory) {
        if (!(inventory.getHolder() instanceof StorageHolder holder)) {
            return;
        }
        saveContents(holder.ownerId(), player.getName(), inventory);
        Inventory tracked = openInventories.get(holder.ownerId());
        if (tracked == inventory) {
            openInventories.remove(holder.ownerId());
        }
        config.playSound(player, "close");
        config.send(player, "saved", "");
    }

    public void reloadConfig() {
        config.reload();
    }

    public void shutdown() {
        for (Map.Entry<UUID, Inventory> entry : Map.copyOf(openInventories).entrySet()) {
            UUID playerId = entry.getKey();
            Player online = Bukkit.getPlayer(playerId);
            String playerName = online == null ? resolveName(playerId) : online.getName();
            saveContents(playerId, playerName, entry.getValue());
        }
        openInventories.clear();
        saveData();
    }

    private ItemStack[] loadContents(UUID playerId, int size) {
        ItemStack[] contents = new ItemStack[size];
        String base = "players." + playerId + ".contents";
        for (int slot = 0; slot < size; slot++) {
            contents[slot] = data.getItemStack(base + "." + slot);
        }
        return contents;
    }

    private void saveOpenInventory(UUID playerId) {
        Inventory inventory = openInventories.get(playerId);
        if (inventory == null) {
            return;
        }
        Player online = Bukkit.getPlayer(playerId);
        saveContents(playerId, online == null ? resolveName(playerId) : online.getName(), inventory);
    }

    private void saveContents(UUID playerId, String playerName, Inventory inventory) {
        String base = "players." + playerId;
        data.set(base + ".name", playerName);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            data.set(base + ".contents." + slot, isEmpty(item) ? null : item);
        }
        saveData();
    }

    private void saveData() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("storagedata.yml 저장 실패: " + e.getMessage());
        }
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private String resolveName(UUID playerId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return offlinePlayer.getName() == null ? playerId.toString() : offlinePlayer.getName();
    }

    public static final class StorageHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private StorageHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        private UUID ownerId() {
            return ownerId;
        }

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
