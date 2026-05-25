package org.kkaemok.dongwon.guild;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public final class GuildJoinNotifyListener implements Listener {
    private final GuildManager guildManager;

    public GuildJoinNotifyListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (guildManager.isAvailable()) {
            return;
        }
        List<String> pendingRequests = guildManager.getPendingJoinRequestNamesForLeader(event.getPlayer().getUniqueId());
        if (!pendingRequests.isEmpty()) {
            guildManager.sendConfigured(event.getPlayer(), "pending-requests-header", "&e[길드] 처리 대기 중인 가입 요청이 있습니다.");
            for (String pending : pendingRequests) {
                guildManager.sendConfigured(event.getPlayer(), "pending-requests-line", "&7- %request%",
                        org.kkaemok.dongwon.text.ConfigText.placeholder("request", pending));
            }
            guildManager.sendConfigured(event.getPlayer(), "pending-requests-actions", "&7수락: /길드 수락 <플레이어> | 거절: /길드 거절 <플레이어>");
        }
        guildManager.getInviteMessage(event.getPlayer().getUniqueId())
                .ifPresent(event.getPlayer()::sendMessage);
    }
}
