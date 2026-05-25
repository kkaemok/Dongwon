package org.kkaemok.dongwon.job;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class JobCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "dongwon.op";

    private final JobManager jobManager;
    private final JobService jobService;

    public JobCommand(JobManager jobManager, JobService jobService) {
        this.jobManager = jobManager;
        this.jobService = jobService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String @NotNull [] args
    ) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("직업설정")) {
            handleSetJob(sender, args);
        } else {
            handleJob(sender, args);
        }
        return true;
    }

    private void handleJob(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return;
        }

        if (args.length == 0) {
            JobType current = jobManager.getJob(player.getUniqueId());
            sender.sendMessage("현재 직업: " + current.getDisplayName());
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("목록")) {
            sender.sendMessage("직업 목록: " + JobType.assignableNames());
            return;
        }

        if (!player.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("권한이 없습니다.");
            return;
        }

        JobType jobType = parseJob(args[0], sender);
        if (jobType == null) {
            return;
        }

        jobManager.setJob(player.getUniqueId(), jobType);
        jobService.applyJobPassivesImmediately(player);
        sender.sendMessage("직업이 변경되었습니다: " + jobType.getDisplayName());
    }

    private void handleSetJob(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("사용법: /직업설정 <플레이어> <직업>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("해당 플레이어가 접속 중이 아닙니다.");
            return;
        }

        JobType jobType = parseJob(args[1], sender);
        if (jobType == null) {
            return;
        }

        jobManager.setJob(target.getUniqueId(), jobType);
        jobService.applyJobPassivesImmediately(target);
        sender.sendMessage(target.getName() + " 직업 변경: " + jobType.getDisplayName());
        target.sendMessage("직업이 변경되었습니다: " + jobType.getDisplayName());
    }

    private JobType parseJob(String raw, CommandSender sender) {
        JobType jobType = JobType.fromInput(raw).orElse(null);
        if (jobType == null || !jobType.isAssignable()) {
            sender.sendMessage("알 수 없는 직업입니다. /직업 목록");
            return null;
        }
        return jobType;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            String @NotNull [] args
    ) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("직업설정")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> startsWith(name, args[0]))
                        .collect(Collectors.toList());
            }
            if (args.length == 2) {
                return suggestJobs(args[1]);
            }
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("목록");
            options.addAll(suggestJobs(""));
            return options.stream()
                    .filter(opt -> startsWith(opt, args[0]))
                    .distinct()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> suggestJobs(String prefix) {
        return Arrays.stream(JobType.values())
                .filter(JobType::isAssignable)
                .map(JobType::getKey)
                .filter(job -> startsWith(job, prefix))
                .collect(Collectors.toList());
    }

    private boolean startsWith(String text, String prefix) {
        return text.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
