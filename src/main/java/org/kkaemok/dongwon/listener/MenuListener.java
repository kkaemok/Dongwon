package org.kkaemok.dongwon.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.kkaemok.dongwon.menu.MenuService;

public final class MenuListener implements Listener {
    private final MenuService menuService;

    public MenuListener(MenuService menuService) {
        this.menuService = menuService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        menuService.giveCompassIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!menuService.isMenuCompass(item)) {
            return;
        }
        event.setCancelled(true);
        menuService.openMainMenu(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        menuService.handleInventoryClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        menuService.handleInventoryDrag(event);
    }
}
