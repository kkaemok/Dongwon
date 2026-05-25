package org.kkaemok.dongwon.level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kkaemok.dongwon.party.PartyManager;

public final class LevelListener implements Listener {
    private final LevelService levelService;
    private final PartyManager partyManager;

    public LevelListener(LevelService levelService, PartyManager partyManager) {
        this.levelService = levelService;
        this.partyManager = partyManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        double exp = levelService.getKillExp(event.getEntity());
        if (exp <= 0.0D) {
            return;
        }

        levelService.addExp(killer, exp);
        for (Player member : partyManager.getOnlineMembersExcept(killer)) {
            levelService.addExp(member, exp * 0.1D);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        levelService.clearBossBar(event.getPlayer());
    }
}
