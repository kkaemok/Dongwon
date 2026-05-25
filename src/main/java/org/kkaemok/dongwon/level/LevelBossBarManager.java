package org.kkaemok.dongwon.level;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LevelBossBarManager {
    private final Plugin plugin;
    private final BossBarConfig config;
    private final Map<UUID, ActiveBar> activeBars = new HashMap<>();

    public LevelBossBarManager(Plugin plugin, BossBarConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void show(Player player, Component title, float progress) {
        clear(player);

        BossBar bossBar = BossBar.bossBar(title, Math.clamp(progress, 0.0F, 1.0F), config.color(), config.overlay());
        player.showBossBar(bossBar);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> clear(player), config.durationTicks());
        activeBars.put(player.getUniqueId(), new ActiveBar(bossBar, task));
    }

    public void clear(Player player) {
        ActiveBar activeBar = activeBars.remove(player.getUniqueId());
        if (activeBar == null) {
            return;
        }
        activeBar.task().cancel();
        player.hideBossBar(activeBar.bossBar());
    }

    public void shutdown() {
        for (ActiveBar activeBar : activeBars.values()) {
            activeBar.task().cancel();
        }
        activeBars.clear();
    }

    private record ActiveBar(BossBar bossBar, BukkitTask task) {
    }
}
