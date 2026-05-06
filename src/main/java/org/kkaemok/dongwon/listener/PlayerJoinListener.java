package org.kkaemok.dongwon.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;

public final class PlayerJoinListener implements Listener {
    private final Plugin plugin;
    private final JobManager jobManager;
    private final JobService jobService;

    public PlayerJoinListener(Plugin plugin, JobManager jobManager, JobService jobService) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.jobService = jobService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (jobManager.getJob(player.getUniqueId()).isAssignable()) {
            runDelayedApply(player, 1L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        runDelayedApply(event.getPlayer(), 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        runDelayedApply(player, 1L);
    }

    private void runDelayedApply(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> jobService.applyJobPassivesImmediately(player), delayTicks);
    }
}
