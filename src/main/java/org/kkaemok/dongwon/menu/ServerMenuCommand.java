package org.kkaemok.dongwon.menu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.text.ConfigText;

public final class ServerMenuCommand implements CommandExecutor {
    private final ServerMenuManager serverMenuManager;
    private final ConfigText text;

    public ServerMenuCommand(ServerMenuManager serverMenuManager, ConfigText text) {
        this.serverMenuManager = serverMenuManager;
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
        serverMenuManager.openMainMenu(player);
        return true;
    }
}
