package org.kkaemok.dongwon.storage;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class StorageListener implements Listener {
    private final StorageManager storageManager;

    public StorageListener(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!storageManager.isStorage(event.getInventory())) {
            return;
        }
        storageManager.handleClose(player, event.getInventory());
    }
}
