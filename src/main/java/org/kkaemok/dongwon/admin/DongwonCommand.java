package org.kkaemok.dongwon.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kkaemok.dongwon.guild.Guild;
import org.kkaemok.dongwon.guild.GuildManager;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;
import org.kkaemok.dongwon.job.JobType;
import org.kkaemok.dongwon.progression.PlayerProfile;
import org.kkaemok.dongwon.progression.ProfileManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DongwonCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "dongwon.op";

    private final JobManager jobManager;
    private final JobService jobService;
    private final ProfileManager profileManager;
    private final GuildManager guildManager;
    private final Runnable reloadAction;

    public DongwonCommand(
            JobManager jobManager,
            JobService jobService,
            ProfileManager profileManager,
            GuildManager guildManager,
            Runnable reloadAction
    ) {
        this.jobManager = jobManager;
        this.jobService = jobService;
        this.profileManager = profileManager;
        this.guildManager = guildManager;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String @NotNull [] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "직업설정" -> handleSetJob(sender, args);
            case "캔설정" -> handleSetCurrency(sender, args);
            case "캔지급" -> handleGiveCurrency(sender, args);
            case "길드관리" -> handleGuildManage(sender, args);
            case "리로드", "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        try {
            reloadAction.run();
            sender.sendMessage("§a동원 플러그인 설정을 다시 불러왔습니다.");
        } catch (RuntimeException ex) {
            sender.sendMessage("§c설정 리로드 중 오류가 발생했습니다. 콘솔을 확인해 주세요.");
            Bukkit.getLogger().warning("[Dongwon] 설정 리로드 실패: " + ex.getMessage());
        }
    }

    private void handleSetJob(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /동원 직업설정 <플레이어> <직업>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§c해당 플레이어는 접속 중이 아닙니다.");
            return;
        }

        JobType jobType = JobType.fromInput(args[2]).orElse(null);
        if (jobType == null || !jobType.isAssignable()) {
            sender.sendMessage("§c직업이 올바르지 않습니다.");
            return;
        }

        jobManager.setJob(target.getUniqueId(), jobType);
        jobService.applyJobPassivesImmediately(target);
        sender.sendMessage("§a" + target.getName() + " 직업 설정 완료: " + jobType.getDisplayName());
        target.sendMessage("§e관리자에 의해 직업이 변경되었습니다: " + jobType.getDisplayName());
    }

    private void handleSetCurrency(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /동원 캔설정 <플레이어> <캔|실버캔|황금캔> <갯수>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§c해당 플레이어는 접속 중이 아닙니다.");
            return;
        }

        CurrencyType currencyType = CurrencyType.fromInput(args[2]);
        if (currencyType == null) {
            sender.sendMessage("§c화폐 종류가 올바르지 않습니다. (캔|실버캔|황금캔)");
            return;
        }

        Long amount = parseAmount(args[3], sender);
        if (amount == null) {
            return;
        }

        PlayerProfile profile = profileManager.get(target.getUniqueId());
        switch (currencyType) {
            case CAN -> profile.setCan(amount);
            case SILVER_CAN -> profile.setSilverCan(amount);
            case GOLDEN_CAN -> profile.setGoldenCan(amount);
        }
        profileManager.save();

        sender.sendMessage("§a" + target.getName() + " " + currencyType.displayName + " 설정: " + amount);
        target.sendMessage("§e관리자에 의해 " + currencyType.displayName + " 수량이 설정되었습니다: " + amount);
    }

    private void handleGiveCurrency(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /동원 캔지급 <플레이어> <캔|실버캔|황금캔> <갯수>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§c해당 플레이어는 접속 중이 아닙니다.");
            return;
        }

        CurrencyType currencyType = CurrencyType.fromInput(args[2]);
        if (currencyType == null) {
            sender.sendMessage("§c화폐 종류가 올바르지 않습니다. (캔|실버캔|황금캔)");
            return;
        }

        Long amount = parseAmount(args[3], sender);
        if (amount == null) {
            return;
        }

        PlayerProfile profile = profileManager.get(target.getUniqueId());
        switch (currencyType) {
            case CAN -> profile.addCan(amount);
            case SILVER_CAN -> profile.addSilverCan(amount);
            case GOLDEN_CAN -> profile.setGoldenCan(profile.getGoldenCan() + amount);
        }
        profileManager.save();

        sender.sendMessage("§a" + target.getName() + " 에게 " + currencyType.displayName + " 지급: " + amount);
        target.sendMessage("§e관리자에 의해 " + currencyType.displayName + " " + amount + "개 지급되었습니다.");
    }

    private void handleGuildManage(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /동원 길드관리 <길드이름> <이름설정|색설정|플레이어설정> <값>");
            sender.sendMessage("§c플레이어설정: /동원 길드관리 <길드이름> 플레이어설정 <플레이어> [추방]");
            return;
        }

        String guildName = args[1];
        String manageType = args[2].toLowerCase(Locale.ROOT);

        switch (manageType) {
            case "이름설정" -> guildManager.adminSetGuildName(guildName, args[3]).sendTo(sender);
            case "색설정" -> guildManager.adminSetGuildColor(guildName, args[3]).sendTo(sender);
            case "플레이어설정" -> {
                if (args.length >= 5) {
                    guildManager.adminSetGuildPlayer(guildName, args[3], args[4]).sendTo(sender);
                } else {
                    if ("추방".equalsIgnoreCase(args[3])) {
                        sender.sendMessage("§c사용법: /동원 길드관리 <길드이름> 플레이어설정 <플레이어> [추방]");
                        return;
                    }
                    guildManager.adminSetGuildPlayer(guildName, args[3], "추방").sendTo(sender);
                }
            }
            default -> sender.sendMessage("§c길드관리 하위 명령은 이름설정, 색설정, 플레이어설정만 사용 가능합니다.");
        }
    }

    private @Nullable Long parseAmount(String raw, CommandSender sender) {
        try {
            long parsed = Long.parseLong(raw);
            if (parsed < 0L) {
                sender.sendMessage("§c갯수는 0 이상이어야 합니다.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            sender.sendMessage("§c갯수는 숫자로 입력해 주세요.");
            return null;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/동원 직업설정 <플레이어> <직업>");
        sender.sendMessage("§e/동원 캔설정 <플레이어> <캔|실버캔|황금캔> <갯수>");
        sender.sendMessage("§e/동원 캔지급 <플레이어> <캔|실버캔|황금캔> <갯수>");
        sender.sendMessage("§e/동원 길드관리 <길드이름> <이름설정|색설정|플레이어설정> <값>");
        sender.sendMessage("§e/동원 리로드");
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            String @NotNull [] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Stream.of("직업설정", "캔설정", "캔지급", "길드관리", "리로드")
                    .filter(option -> startsWith(option, args[0]))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if ((sub.equals("직업설정") || sub.equals("캔설정") || sub.equals("캔지급")) && args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> startsWith(name, args[1]))
                    .collect(Collectors.toList());
        }

        if (sub.equals("직업설정") && args.length == 3) {
            return Arrays.stream(JobType.values())
                    .filter(JobType::isAssignable)
                    .map(JobType::getKey)
                    .filter(job -> startsWith(job, args[2]))
                    .collect(Collectors.toList());
        }

        if ((sub.equals("캔설정") || sub.equals("캔지급")) && args.length == 3) {
            return Arrays.stream(CurrencyType.values())
                    .map(type -> type.displayName)
                    .filter(name -> startsWith(name, args[2]))
                    .collect(Collectors.toList());
        }

        if (sub.equals("길드관리")) {
            if (args.length == 2) {
                return guildManager.getGuilds().stream()
                        .map(Guild::getName)
                        .filter(name -> startsWith(name, args[1]))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                return Stream.of("이름설정", "색설정", "플레이어설정")
                        .filter(option -> startsWith(option, args[2]))
                        .collect(Collectors.toList());
            }
            if (args.length == 4) {
                if (args[2].equalsIgnoreCase("색설정")) {
                    return Stream.of("빨강", "노랑", "연두", "하늘", "보라", "흰색", "회색", "&c", "&e", "&a", "&b", "&d")
                            .filter(option -> startsWith(option, args[3]))
                            .collect(Collectors.toList());
                }
                if (args[2].equalsIgnoreCase("플레이어설정")) {
                    return guildManager.getGuildMemberNames(args[1]).stream()
                            .filter(name -> startsWith(name, args[3]))
                            .collect(Collectors.toList());
                }
            }
            if (args.length == 5 && args[2].equalsIgnoreCase("플레이어설정")) {
                return Stream.of("추방")
                        .filter(option -> startsWith(option, args[4]))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    private enum CurrencyType {
        CAN("캔"),
        SILVER_CAN("실버캔"),
        GOLDEN_CAN("황금캔");

        private final String displayName;

        CurrencyType(String displayName) {
            this.displayName = displayName;
        }

        private static @Nullable CurrencyType fromInput(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            return Arrays.stream(values())
                    .filter(type -> type.displayName.equalsIgnoreCase(input.trim()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
