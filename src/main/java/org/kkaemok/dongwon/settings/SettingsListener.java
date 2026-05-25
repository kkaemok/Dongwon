package org.kkaemok.dongwon.settings;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class SettingsListener implements Listener {
    private final SettingsMenu settingsMenu;

    public SettingsListener(SettingsMenu settingsMenu) {
        this.settingsMenu = settingsMenu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        settingsMenu.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        settingsMenu.handleDrag(event);
    }
}
