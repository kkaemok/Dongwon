package org.kkaemok.dongwon.rtp;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RtpListener implements Listener {
    private final RtpManager rtpManager;

    public RtpListener(RtpManager rtpManager) {
        this.rtpManager = rtpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        rtpManager.onPlayerMove(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        rtpManager.onQuit(event.getPlayer());
    }
}
