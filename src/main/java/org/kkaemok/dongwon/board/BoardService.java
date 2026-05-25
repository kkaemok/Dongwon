package org.kkaemok.dongwon.board;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobType;
import org.kkaemok.dongwon.level.LevelExpFormatter;
import org.kkaemok.dongwon.party.PartyManager;
import org.kkaemok.dongwon.progression.PlayerProfile;
import org.kkaemok.dongwon.progression.ProfileManager;
import org.kkaemok.dongwon.text.ConfigText;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BoardService {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    private final Plugin plugin;
    private final FastBoardConfig config;
    private final JobManager jobManager;
    private final ProfileManager profileManager;
    private final PartyManager partyManager;
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private BukkitTask updateTask;

    public BoardService(
            Plugin plugin,
            FastBoardConfig config,
            JobManager jobManager,
            ProfileManager profileManager,
            PartyManager partyManager
    ) {
        this.plugin = plugin;
        this.config = config;
        this.jobManager = jobManager;
        this.profileManager = profileManager;
        this.partyManager = partyManager;
    }

    public void start() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, config.updateIntervalTicks());
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

        ConfigText.Placeholder[] placeholders = {
                ConfigText.placeholder("player", player.getName()),
                ConfigText.placeholder("job", jobType.getColoredDisplayName()),
                ConfigText.placeholder("guild", profile.getGuildName()),
                ConfigText.placeholder("can", formatNumber(profile.getCan())),
                ConfigText.placeholder("silver_can", formatNumber(profile.getSilverCan())),
                ConfigText.placeholder("golden_can", formatNumber(profile.getGoldenCan())),
                ConfigText.placeholder("level", profile.getLevel()),
                ConfigText.placeholder("level_exp", LevelExpFormatter.format(profile.getLevelExp())),
                ConfigText.placeholder("playtime", formatPlaytime(player))
        };

        board.updateTitle(config.title(placeholders));
        board.updateLines(config.lines(partyManager.scoreboardLines(player), placeholders));
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
