package org.kkaemok.dongwon.settings;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.text.ConfigText;

public final class SettingsCommand implements CommandExecutor {
    private final SettingsMenu settingsMenu;
    private final ConfigText text;

    public SettingsCommand(SettingsMenu settingsMenu, ConfigText text) {
        this.settingsMenu = settingsMenu;
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
        settingsMenu.open(player);
        return true;
    }
}
