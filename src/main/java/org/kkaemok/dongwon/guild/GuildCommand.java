package org.kkaemok.dongwon.guild;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class GuildCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "dongwon.op";
    private static final List<String> SUB_COMMANDS = List.of(
            "생성", "삭제", "탈퇴", "정보", "퇴출", "요청", "가입", "목록", "수락", "거절", "초대"
    );

    private final GuildManager guildManager;

    public GuildCommand(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            guildManager.sendConfigured(sender, "player-only", "<red>플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean isAdmin = player.hasPermission(ADMIN_PERMISSION);

        switch (sub) {
            case "생성" -> {
                if (args.length < 3) {
                    guildManager.sendConfigured(player, "usage-create", "&c사용법: /길드 생성 <이름> <색>");
                    return true;
                }
                guildManager.createGuild(player, args[1], args[2]).sendTo(player);
            }
            case "삭제" -> {
                String targetName = args.length >= 2 ? args[1] : null;
                guildManager.deleteGuild(player, targetName, isAdmin).sendTo(player);
            }
            case "탈퇴" -> guildManager.leaveGuild(player).sendTo(player);
            case "정보" -> handleInfo(player, args);
            case "퇴출" -> {
                if (args.length < 2) {
                    guildManager.sendConfigured(player, "usage-kick", "&c사용법: /길드 퇴출 <플레이어>");
                    return true;
                }
                guildManager.kickMember(player, args[1], isAdmin).sendTo(player);
            }
            case "요청", "가입" -> {
                if (args.length < 2) {
                    guildManager.sendConfigured(player, "usage-join", "&c사용법: /길드 %command% <길드명>",
                            placeholder("command", sub));
                    return true;
                }
                guildManager.requestJoin(player, args[1]).sendTo(player);
            }
            case "목록" -> guildManager.getGuildListLines().forEach(player::sendMessage);
            case "초대" -> {
                if (args.length < 2) {
                    guildManager.sendConfigured(player, "usage-invite", "&c사용법: /길드 초대 <플레이어>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    guildManager.sendConfigured(player, "target-offline", "&c해당 플레이어는 접속 중이 아닙니다.");
                    return true;
                }
                guildManager.invitePlayer(player, target).sendTo(player);
            }
            case "수락" -> handleAcceptOrDeny(player, args, true);
            case "거절" -> handleAcceptOrDeny(player, args, false);
            default -> sendUsage(player);
        }
        return true;
    }

    private void handleInfo(Player player, String[] args) {
        Guild guild;
        if (args.length >= 2) {
            guild = guildManager.getGuildByName(args[1]).orElse(null);
            if (guild == null) {
                guildManager.sendConfigured(player, "not-found", "&c해당 길드를 찾을 수 없습니다.");
                return;
            }
        } else {
            guild = guildManager.getGuildOf(player.getUniqueId()).orElse(null);
            if (guild == null) {
                guildManager.sendConfigured(player, "not-in-guild", "&c소속된 길드가 없습니다.");
                return;
            }
        }
        guildManager.getGuildInfoLines(guild).forEach(player::sendMessage);
    }

    private void handleAcceptOrDeny(Player player, String[] args, boolean accept) {
        String target = args.length >= 2 ? args[1] : "";

        if (guildManager.isGuildLeader(player.getUniqueId())) {
            if (!guildManager.hasPendingJoinRequests(player.getUniqueId())) {
                guildManager.sendConfigured(player, "no-join-requests", "&c처리할 길드 가입 요청이 없습니다.");
                return;
            }
            guildManager.acceptOrDenyJoinRequest(player, target, accept).sendTo(player);
            return;
        }

        GuildManager.Result inviteResult = guildManager.acceptOrDenyInvite(player, target, accept);
        inviteResult.sendTo(player);
    }

    private void sendUsage(Player player) {
        Stream.of(
                "usage-main",
                "usage-create",
                "usage-invite",
                "usage-join"
        ).forEach(key -> {
            String fallback = switch (key) {
                case "usage-create" -> "&e/길드 생성 <이름> <색>";
                case "usage-invite" -> "&e/길드 초대 <플레이어>";
                case "usage-join" -> "&e/길드 요청 <길드명>";
                default -> "&e/길드 <생성|삭제|탈퇴|정보|퇴출|요청|가입|목록|수락|거절|초대>";
            };
            guildManager.sendConfigured(player, key, fallback, placeholder("command", "요청"));
        });
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            String @NotNull [] args
    ) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(value -> startsWith(value, args[0]))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("초대") || sub.equals("퇴출") || sub.equals("수락") || sub.equals("거절")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> startsWith(name, args[1]))
                        .collect(Collectors.toList());
            }
            if (sub.equals("요청") || sub.equals("가입") || sub.equals("정보") || sub.equals("삭제")) {
                return guildManager.getGuilds().stream()
                        .map(Guild::getName)
                        .filter(name -> startsWith(name, args[1]))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("생성")) {
            return Stream.of("빨강", "노랑", "연두", "하늘", "보라", "흰색", "회색", "&c", "&e", "&a", "&b", "&d")
                    .filter(color -> startsWith(color, args[2]))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
