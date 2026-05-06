package org.kkaemok.dongwon.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kkaemok.dongwon.teleport.TpaService;

public final class TpaListener implements Listener {
    private final TpaService tpaService;

    public TpaListener(TpaService tpaService) {
        this.tpaService = tpaService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tpaService.onQuit(event.getPlayer());
    }
}
