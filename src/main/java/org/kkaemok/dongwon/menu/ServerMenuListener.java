package org.kkaemok.dongwon.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class ServerMenuListener implements Listener {
    private final Plugin plugin;
    private final ServerMenuManager serverMenuManager;

    public ServerMenuListener(Plugin plugin, ServerMenuManager serverMenuManager) {
        this.plugin = plugin;
        this.serverMenuManager = serverMenuManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        serverMenuManager.giveCompassIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> serverMenuManager.giveCompassIfMissing(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(serverMenuManager::isMenuCompass);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!serverMenuManager.isMenuCompass(event.getItemDrop().getItemStack())) {
            return;
        }
        event.setCancelled(true);
        serverMenuManager.sendCompassDropBlocked(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!serverMenuManager.isMenuCompass(item)) {
            return;
        }
        event.setCancelled(true);
        serverMenuManager.openMainMenu(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isMenuCompassDropClick(event)) {
            event.setCancelled(true);
            serverMenuManager.sendCompassDropBlocked(player);
            return;
        }
        serverMenuManager.handleInventoryClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        serverMenuManager.handleInventoryDrag(event);
    }

    private boolean isMenuCompassDropClick(InventoryClickEvent event) {
        ClickType click = event.getClick();
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            return serverMenuManager.isMenuCompass(event.getCurrentItem());
        }
        if (click == ClickType.WINDOW_BORDER_LEFT || click == ClickType.WINDOW_BORDER_RIGHT) {
            return serverMenuManager.isMenuCompass(event.getCursor());
        }
        return false;
    }
}
