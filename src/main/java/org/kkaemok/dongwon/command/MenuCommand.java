package org.kkaemok.dongwon.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.menu.MenuService;

public final class MenuCommand implements CommandExecutor {
    private final MenuService menuService;

    public MenuCommand(MenuService menuService) {
        this.menuService = menuService;
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
        menuService.openMainMenu(player);
        return true;
    }
}
