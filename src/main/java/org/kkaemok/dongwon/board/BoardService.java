package org.kkaemok.dongwon.board;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobType;
import org.kkaemok.dongwon.progression.PlayerProfile;
import org.kkaemok.dongwon.progression.ProfileManager;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BoardService {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    private final Plugin plugin;
    private final JobManager jobManager;
    private final ProfileManager profileManager;
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private BukkitTask updateTask;

    public BoardService(Plugin plugin, JobManager jobManager, ProfileManager profileManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.profileManager = profileManager;
    }

    public void start() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, 20L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            createBoard(player);
        }
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (FastBoard board : boards.values()) {
            board.delete();
        }
        boards.clear();
    }

    public void createBoard(Player player) {
        FastBoard existing = boards.remove(player.getUniqueId());
        if (existing != null) {
            existing.delete();
        }
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        updateBoard(board);
    }

    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    private void updateAll() {
        boards.values().removeIf(board -> {
            Player player = board.getPlayer();
            if (player == null || !player.isOnline()) {
                board.delete();
                return true;
            }
            updateBoard(board);
            return false;
        });
    }

    private void updateBoard(FastBoard board) {
        Player player = board.getPlayer();
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        PlayerProfile profile = profileManager.get(playerId);
        JobType jobType = jobManager.getJob(playerId);

        board.updateTitle("§6§lDongwon");
        board.updateLines(
                "§7이름 §f" + player.getName(),
                "§7직업 §f" + jobType.getDisplayName(),
                "§7길드 §f" + profile.getGuildName(),
                "",
                "§6황금캔 §f" + formatNumber(profile.getGoldenCan()),
                "§7실버캔 §f" + formatNumber(profile.getSilverCan()),
                "§f캔 §f" + formatNumber(profile.getCan()),
                "",
                "§a플레이타임",
                "§f" + formatPlaytime(player)
        );
    }

    private String formatPlaytime(Player player) {
        long playSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
        long days = playSeconds / 86_400L;
        long hours = (playSeconds % 86_400L) / 3_600L;
        long minutes = (playSeconds % 3_600L) / 60L;
        long seconds = playSeconds % 60L;
        if (days > 0) {
            return String.format("%d일 %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatNumber(long value) {
        return NUMBER_FORMAT.format(Math.max(0L, value));
    }
}
