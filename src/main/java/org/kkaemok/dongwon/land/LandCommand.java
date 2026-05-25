package org.kkaemok.dongwon.land;

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

public final class LandCommand implements CommandExecutor, TabCompleter {
    private final LandManager landManager;
    private final ConfigText text;

    public LandCommand(LandManager landManager, ConfigText text) {
        this.landManager = landManager;
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

        switch (command.getName()) {
            case "땅설정" -> handleCreate(player, args);
            case "땅경계" -> handleBoundary(player, args);
            case "땅삭제" -> handleDelete(player, args);
            case "땅추방" -> handleKick(player, args);
            case "허락요청" -> landManager.requestAccess(player);
            case "허락수락" -> handleAccept(player, args);
            case "땅양도" -> handleTransfer(player, args);
            case "땅목록" -> landManager.list(player);
            case "땅정보" -> landManager.info(player, args.length == 0 ? null : args[0]);
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
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        String commandName = command.getName();
        if (args.length == 1) {
            return switch (commandName) {
                case "땅경계", "땅정보" -> filter(landManager.allLandNames(), args[0]);
                case "땅삭제", "땅추방", "허락수락", "땅양도" -> filter(landManager.ownedLandNames(player), args[0]);
                default -> List.of();
            };
        }
        if (args.length == 2) {
            return switch (commandName) {
                case "땅추방" -> filter(landManager.memberNames(player, args[0]), args[1]);
                case "허락수락", "땅양도" -> filter(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList(), args[1]);
                default -> List.of();
            };
        }
        return List.of();
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 1) {
            text.send(player, "land.messages.usage-create", "<yellow>사용법: /땅설정 <땅이름>");
            return;
        }
        landManager.create(player, args[0]);
    }

    private void handleBoundary(Player player, String[] args) {
        if (args.length < 1) {
            text.send(player, "land.messages.usage-boundary", "<yellow>사용법: /땅경계 <땅이름>");
            return;
        }
        landManager.showBoundary(player, args[0]);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 1) {
            text.send(player, "land.messages.usage-delete", "<yellow>사용법: /땅삭제 <땅이름>");
            return;
        }
        landManager.delete(player, args[0]);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            text.send(player, "land.messages.usage-kick", "<yellow>사용법: /땅추방 <땅이름> <플레이어>");
            return;
        }
        landManager.kick(player, args[0], args[1]);
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            text.send(player, "land.messages.usage-accept", "<yellow>사용법: /허락수락 <땅이름> <플레이어>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            text.send(player, "land.messages.target-offline", "<red>해당 플레이어는 접속 중이 아닙니다.");
            return;
        }
        landManager.acceptRequest(player, args[0], target);
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            text.send(player, "land.messages.usage-transfer", "<yellow>사용법: /땅양도 <땅이름> <플레이어>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            text.send(player, "land.messages.target-offline", "<red>해당 플레이어는 접속 중이 아닙니다.");
            return;
        }
        landManager.transfer(player, args[0], target);
    }

    private void sendUsage(Player player) {
        text.send(player, "land.messages.usage-create", "<yellow>사용법: /땅설정 <땅이름>");
        text.send(player, "land.messages.usage-boundary", "<yellow>사용법: /땅경계 <땅이름>");
        text.send(player, "land.messages.usage-delete", "<yellow>사용법: /땅삭제 <땅이름>");
        text.send(player, "land.messages.usage-request", "<yellow>사용법: /허락요청");
        text.send(player, "land.messages.usage-accept", "<yellow>사용법: /허락수락 <땅이름> <플레이어>");
        text.send(player, "land.messages.usage-list", "<yellow>사용법: /땅목록");
        text.send(player, "land.messages.usage-info", "<yellow>사용법: /땅정보 [땅이름]");
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }
}
