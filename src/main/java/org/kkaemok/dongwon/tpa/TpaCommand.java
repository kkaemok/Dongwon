package org.kkaemok.dongwon.tpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.settings.PlayerSettingsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TpaCommand implements CommandExecutor, TabCompleter {
    private final TpaManager tpaManager;
    private final TpaConfig config;
    private final TpaMenu tpaMenu;
    private final PlayerSettingsManager settingsManager;

    public TpaCommand(
            TpaManager tpaManager,
            TpaConfig config,
            TpaMenu tpaMenu,
            PlayerSettingsManager settingsManager
    ) {
        this.tpaManager = tpaManager;
        this.config = config;
        this.tpaMenu = tpaMenu;
        this.settingsManager = settingsManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("player-only", "<red>플레이어만 사용할 수 있습니다."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(config.message("usage-request", "<red>사용법: /tpa \\<플레이어>"));
            player.sendMessage(config.message("usage-reply", "<red>사용법: /tpa \\<수락|거절> [플레이어]"));
            return true;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("수락")) {
            String requesterName = args.length >= 2 ? args[1] : "";
            tpaManager.accept(player, requesterName).send(player);
            return true;
        }
        if (first.equals("거절")) {
            String requesterName = args.length >= 2 ? args[1] : "";
            tpaManager.deny(player, requesterName).send(player);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(config.message("target-offline", "<red>해당 플레이어는 접속 중이 아닙니다."));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(config.message("self-request", "<red>자기 자신에게 TPA 요청을 보낼 수 없습니다."));
            return true;
        }
        if (settingsManager.isTpaGuiEnabled(player.getUniqueId())) {
            tpaMenu.open(player, target);
            return true;
        }
        tpaManager.requestTeleport(player, target).send(player);
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
            List<String> options = new ArrayList<>();
            options.add("수락");
            options.add("거절");
            options.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList());
            return options.stream()
                    .filter(value -> startsWith(value, args[0]))
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && sender instanceof Player player) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("수락") || first.equals("거절")) {
                String pendingRequester = tpaManager.getPendingRequesterName(player);
                if (pendingRequester.isBlank()) {
                    return Collections.emptyList();
                }
                if (startsWith(pendingRequester, args[1])) {
                    return List.of(pendingRequester);
                }
            }
        }
        return Collections.emptyList();
    }

    private boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
