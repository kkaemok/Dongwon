package org.kkaemok.dongwon.player;

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
import org.kkaemok.dongwon.progression.ProfileManager;

public final class PlayerJoinListener implements Listener {
    private final Plugin plugin;
    private final JobManager jobManager;
    private final JobService jobService;
    private final ProfileManager profileManager;

    public PlayerJoinListener(Plugin plugin, JobManager jobManager, JobService jobService, ProfileManager profileManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.jobService = jobService;
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profileManager.setPlayerName(player.getUniqueId(), player.getName());
        profileManager.save();
        if (jobManager.getJob(player.getUniqueId()).isAssignable()) {
            runDelayedApply(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        runDelayedApply(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        runDelayedApply(player);
    }

    private void runDelayedApply(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> jobService.applyJobPassivesImmediately(player), 1L);
    }
}
