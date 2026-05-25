package org.kkaemok.dongwon.land;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class LandListener implements Listener {
    private final LandManager landManager;

    public LandListener(LandManager landManager) {
        this.landManager = landManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || !changedBlock(from, to)) {
            return;
        }
        landManager.handleMove(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        LandClaim claim = landManager.landAt(event.getBlock().getLocation()).orElse(null);
        if (claim == null || landManager.canUse(event.getPlayer(), claim)) {
            return;
        }

        event.setCancelled(true);
        landManager.deny(event.getPlayer(), claim, "break-denied", "<red>[%owner%]님의 땅에서는 블록을 파괴할 수 없습니다.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        LandClaim claim = landManager.landAt(event.getBlock().getLocation()).orElse(null);
        if (claim == null || landManager.canUse(event.getPlayer(), claim)) {
            return;
        }

        event.setCancelled(true);
        landManager.deny(event.getPlayer(), claim, "place-denied", "<red>[%owner%]님의 땅에서는 블록을 설치할 수 없습니다.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        LandClaim claim = landManager.landAt(clicked.getLocation()).orElse(null);
        if (claim == null || landManager.canUse(event.getPlayer(), claim)) {
            return;
        }

        event.setCancelled(true);
        landManager.deny(event.getPlayer(), claim, "interact-denied", "<red>[%owner%]님의 허락 없이 상호작용할 수 없습니다.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        LandClaim claim = landManager.landAt(event.getBlock().getLocation()).orElse(null);
        if (claim == null || landManager.canUse(event.getPlayer(), claim)) {
            return;
        }

        event.setCancelled(true);
        landManager.deny(event.getPlayer(), claim, "place-denied", "<red>[%owner%]님의 땅에서는 블록을 설치할 수 없습니다.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        LandClaim claim = landManager.landAt(event.getBlock().getLocation()).orElse(null);
        if (claim == null || landManager.canUse(event.getPlayer(), claim)) {
            return;
        }

        event.setCancelled(true);
        landManager.deny(event.getPlayer(), claim, "break-denied", "<red>[%owner%]님의 땅에서는 블록을 파괴할 수 없습니다.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> landManager.landAt(block.getLocation()).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> landManager.landAt(block.getLocation()).isPresent());
    }

    private boolean changedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || from.getWorld() != to.getWorld();
    }
}
