package org.kkaemok.dongwon.party;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.text.ConfigText;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PartyCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUB_COMMANDS = List.of("생성", "초대", "수락", "거절", "퇴출", "해제", "탈퇴", "정보");

    private final PartyManager partyManager;
    private final ConfigText text;

    public PartyCommand(PartyManager partyManager, ConfigText text) {
        this.partyManager = partyManager;
        this.text = text;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            text.send(sender, "messages.common.player-only", "<red>플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "생성" -> partyManager.create(player);
            case "초대" -> {
                if (args.length < 2) {
                    text.send(player, "messages.party.usage.invite", "<yellow>/파티 초대 <플레이어>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null || !target.isOnline()) {
                    text.send(player, "messages.party.target-offline", "<red>해당 플레이어는 접속 중이 아닙니다.");
                    return true;
                }
                partyManager.invite(player, target);
            }
            case "수락" -> partyManager.accept(player);
            case "거절" -> partyManager.deny(player);
            case "퇴출" -> {
                if (args.length < 2) {
                    text.send(player, "messages.party.usage.kick", "<yellow>/파티 퇴출 <플레이어>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null || !target.isOnline()) {
                    text.send(player, "messages.party.target-offline", "<red>해당 플레이어는 접속 중이 아닙니다.");
                    return true;
                }
                partyManager.kick(player, target);
            }
            case "해제" -> partyManager.disband(player);
            case "탈퇴" -> partyManager.leave(player);
            case "정보" -> partyManager.info(player);
            default -> sendUsage(player);
        }
        return true;
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
        if (args.length == 2 && sender instanceof Player player) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("초대")) {
                return Bukkit.getOnlinePlayers().stream()
                        .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                        .map(Player::getName)
                        .filter(name -> startsWith(name, args[1]))
                        .collect(Collectors.toList());
            }
            if (sub.equals("퇴출")) {
                return partyManager.memberNames(player).stream()
                        .filter(name -> startsWith(name, args[1]))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private void sendUsage(Player player) {
        Stream.of(
                text.component("messages.party.usage.create", "<yellow>/파티 생성"),
                text.component("messages.party.usage.invite", "<yellow>/파티 초대 <플레이어>"),
                text.component("messages.party.usage.accept", "<yellow>/파티 수락"),
                text.component("messages.party.usage.deny", "<yellow>/파티 거절"),
                text.component("messages.party.usage.kick", "<yellow>/파티 퇴출 <플레이어>"),
                text.component("messages.party.usage.disband", "<yellow>/파티 해제"),
                text.component("messages.party.usage.leave", "<yellow>/파티 탈퇴"),
                text.component("messages.party.usage.info", "<yellow>/파티 정보")
        ).forEach(player::sendMessage);
    }

    private boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
