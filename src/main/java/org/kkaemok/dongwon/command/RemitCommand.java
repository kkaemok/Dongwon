package org.kkaemok.dongwon.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kkaemok.dongwon.progression.PlayerProfile;
import org.kkaemok.dongwon.progression.ProfileManager;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RemitCommand implements CommandExecutor, TabCompleter {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    private final ProfileManager profileManager;

    public RemitCommand(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c사용법: /송금 <플레이어> <수량>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c해당 플레이어는 접속 중이 아닙니다.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§c자기 자신에게 송금할 수 없습니다.");
            return true;
        }

        String amountArg;
        if (args.length == 2) {
            amountArg = args[1];
        } else {
            if (!args[1].equalsIgnoreCase("캔")) {
                player.sendMessage("§c실버캔과 황금캔은 송금할 수 없습니다.");
                return true;
            }
            amountArg = args[2];
        }

        long amount;
        try {
            amount = Long.parseLong(amountArg);
            if (amount <= 0L) {
                player.sendMessage("§c수량은 1 이상이어야 합니다.");
                return true;
            }
        } catch (NumberFormatException ex) {
            player.sendMessage("§c수량은 숫자로 입력해야 합니다.");
            return true;
        }

        if (args.length >= 4) {
            player.sendMessage("§c사용법: /송금 <플레이어> <수량>");
            return true;
        }

        PlayerProfile senderProfile = profileManager.get(player.getUniqueId());
        PlayerProfile targetProfile = profileManager.get(target.getUniqueId());

        if (senderProfile.getCan() < amount) {
            player.sendMessage("§c보유한 캔이 부족합니다.");
            return true;
        }

        senderProfile.setCan(senderProfile.getCan() - amount);
        targetProfile.addCan(amount);
        profileManager.save();

        String formatted = NUMBER_FORMAT.format(amount);
        player.sendMessage("§a" + target.getName() + " 님에게 캔 " + formatted + "개를 송금했습니다.");
        target.sendMessage("§e" + player.getName() + " 님이 캔 " + formatted + "개를 송금했습니다.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> startsWith(name, args[0]))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (startsWith("캔", args[1])) {
                return List.of("캔");
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
