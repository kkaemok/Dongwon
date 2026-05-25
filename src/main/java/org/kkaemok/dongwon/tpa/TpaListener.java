package org.kkaemok.dongwon.tpa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;

public final class TpaListener implements Listener {
    private final TpaManager tpaManager;
    private final TpaConfig config;
    private final TpaMenu tpaMenu;

    public TpaListener(TpaManager tpaManager, TpaConfig config, TpaMenu tpaMenu) {
        this.tpaManager = tpaManager;
        this.config = config;
        this.tpaMenu = tpaMenu;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!tpaMenu.isMenu(view.getTopInventory())) {
            return;
        }
        handleMenuClick(event, view);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        if (!tpaMenu.isMenu(view.getTopInventory())) {
            return;
        }

        int topSize = view.getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (tpaMenu.isMenu(event.getInventory())) {
            tpaMenu.onClose(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tpaMenu.untrack(event.getPlayer().getUniqueId());
        tpaManager.onQuit(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        tpaMenu.untrack(event.getPlayer().getUniqueId());
        tpaManager.onQuit(event.getPlayer());
    }

    private void handleMenuClick(InventoryClickEvent event, InventoryView view) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClick().isKeyboardClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == view.getBottomInventory() && event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != view.getTopInventory()) {
            return;
        }

        event.setCancelled(true);
        TpaMenu.Context context = tpaMenu.context(view.getTopInventory());
        if (context == null) {
            return;
        }
        if (!context.viewerId().equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();
        if (slot == TpaMenu.CANCEL_SLOT) {
            if (context.mode() == TpaMenu.Mode.REQUEST_REPLY) {
                player.closeInventory();
                tpaManager.deny(player, requesterName(context)).send(player);
                return;
            }
            player.closeInventory();
            return;
        }
        if (slot != TpaMenu.CONFIRM_SLOT) {
            return;
        }

        if (context.mode() == TpaMenu.Mode.REQUEST_REPLY) {
            player.closeInventory();
            tpaManager.accept(player, requesterName(context)).send(player);
            return;
        }

        Player sender = Bukkit.getPlayer(context.requesterId());
        Player target = Bukkit.getPlayer(context.targetId());
        player.closeInventory();

        if (sender == null || !sender.isOnline() || target == null || !target.isOnline()) {
            player.sendMessage(config.message("target-offline", "<red>해당 플레이어는 접속 중이 아닙니다."));
            return;
        }
        tpaManager.requestTeleport(sender, target).send(sender);
    }

    private String requesterName(TpaMenu.Context context) {
        Player requester = Bukkit.getPlayer(context.requesterId());
        return requester == null ? "" : requester.getName();
    }
}
