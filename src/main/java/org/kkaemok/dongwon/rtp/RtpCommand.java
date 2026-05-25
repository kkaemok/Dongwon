package org.kkaemok.dongwon.rtp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.text.ConfigText;

public final class RtpCommand implements CommandExecutor {
    private final RtpManager rtpManager;
    private final ConfigText text;

    public RtpCommand(RtpManager rtpManager, ConfigText text) {
        this.rtpManager = rtpManager;
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

        rtpManager.start(player).send(player);
        return true;
    }
}
