package org.kkaemok.dongwon.progression;

import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MasteryListener implements Listener {
    private final MasteryService masteryService;

    public MasteryListener(MasteryService masteryService) {
        this.masteryService = masteryService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        masteryService.onMonsterKill(killer);
    }
}
