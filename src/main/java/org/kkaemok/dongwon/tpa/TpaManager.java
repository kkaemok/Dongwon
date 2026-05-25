package org.kkaemok.dongwon.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kkaemok.dongwon.settings.PlayerSettingsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class TpaManager {
    private final TpaConfig config;
    private final TpaMenu menu;
    private final PlayerSettingsManager settingsManager;
    private final Map<UUID, Request> requestsByTarget = new HashMap<>();
    private final Map<UUID, UUID> targetByRequester = new HashMap<>();

    public TpaManager(TpaConfig config, TpaMenu menu, PlayerSettingsManager settingsManager) {
        this.config = config;
        this.menu = menu;
        this.settingsManager = settingsManager;
    }

    public void shutdown() {
        requestsByTarget.clear();
        targetByRequester.clear();
    }

    public Result requestTeleport(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            return fail("self-request", "<red>자기 자신에게 TPA 요청을 보낼 수 없습니다.");
        }

        clearExpiredRequest(target.getUniqueId());
        clearExpiredOutgoingRequest(requester.getUniqueId());
        if (targetByRequester.containsKey(requester.getUniqueId())) {
            return fail("duplicate", "<red>이미 해당 플레이어에게 TPA 요청을 보냈습니다.");
        }

        Request existing = requestsByTarget.get(target.getUniqueId());
        if (existing != null) {
            if (existing.requesterId.equals(requester.getUniqueId())) {
                return fail("duplicate", "<red>이미 해당 플레이어에게 TPA 요청을 보냈습니다.");
            }
            return fail("target-busy", "<red>해당 플레이어는 다른 TPA 요청을 처리 중입니다.");
        }

        Request request = new Request(
                requester.getUniqueId(),
                target.getUniqueId(),
                System.currentTimeMillis() + config.requestTimeoutMillis()
        );
        requestsByTarget.put(target.getUniqueId(), request);
        targetByRequester.put(requester.getUniqueId(), target.getUniqueId());

        target.sendMessage(config.message("request-received",
                "<yellow>[TPA] %requester% 님이 당신에게 이동 요청을 보냈습니다.",
                placeholder("requester", requester.getName())));
        target.sendMessage(config.message("accept-button", "<green>[수락]")
                .clickEvent(ClickEvent.suggestCommand("/tpa 수락 " + requester.getName()))
                .append(Component.space())
                .append(config.message("deny-button", "<red>[거절]")
                        .clickEvent(ClickEvent.suggestCommand("/tpa 거절 " + requester.getName()))));
        if (settingsManager.isTpaGuiEnabled(target.getUniqueId())) {
            menu.openReply(target, requester);
        }
        requester.sendMessage(config.message("request-sent",
                "<green>[TPA] %target% 님에게 요청을 보냈습니다. (%timeout%초)",
                placeholder("target", target.getName()),
                placeholder("timeout", config.requestTimeoutSeconds())));
        return ok("request-complete", "<green>요청 전송 완료");
    }

    public Result accept(Player target, String requesterName) {
        clearExpiredRequest(target.getUniqueId());

        Request request = requestsByTarget.get(target.getUniqueId());
        if (request == null) {
            return fail("no-accept-request", "<red>수락할 TPA 요청이 없습니다.");
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester == null || !requester.isOnline()) {
            removeRequest(request);
            return fail("requester-offline", "<red>요청한 플레이어가 접속 중이 아닙니다.");
        }

        if (requesterName != null && !requesterName.isBlank()
                && !requester.getName().equalsIgnoreCase(requesterName.trim())) {
            return fail("requester-mismatch", "<red>해당 플레이어의 요청이 아닙니다.");
        }

        removeRequest(request);
        requester.teleport(target.getLocation());
        requester.sendMessage(config.message("teleported-to-target",
                "<green>[TPA] %target% 님에게 이동했습니다.",
                placeholder("target", target.getName())));
        target.sendMessage(config.message("accepted-target",
                "<green>[TPA] %requester% 님의 요청을 수락했습니다.",
                placeholder("requester", requester.getName())));
        return ok("accept-complete", "<green>TPA 요청을 수락했습니다.");
    }

    public Result deny(Player target, String requesterName) {
        clearExpiredRequest(target.getUniqueId());
        Request request = requestsByTarget.get(target.getUniqueId());
        if (request == null) {
            return fail("no-deny-request", "<red>거절할 TPA 요청이 없습니다.");
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requesterName != null && !requesterName.isBlank() && requester != null
                && !requester.getName().equalsIgnoreCase(requesterName.trim())) {
            return fail("requester-mismatch", "<red>해당 플레이어의 요청이 아닙니다.");
        }

        removeRequest(request);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(config.message("denied-requester",
                    "<red>[TPA] %target% 님이 요청을 거절했습니다.",
                    placeholder("target", target.getName())));
        }
        return ok("deny-complete", "<green>TPA 요청을 거절했습니다.");
    }

    public void onQuit(Player player) {
        clearExpiredRequest(player.getUniqueId());
        removeIncomingRequest(player.getUniqueId());
        removeOutgoingRequest(player.getUniqueId(), true);
    }

    public String getPendingRequesterName(Player target) {
        clearExpiredRequest(target.getUniqueId());
        Request request = requestsByTarget.get(target.getUniqueId());
        if (request == null) {
            return "";
        }
        Player requester = Bukkit.getPlayer(request.requesterId);
        return requester == null ? "" : requester.getName();
    }

    private void clearExpiredRequest(UUID targetId) {
        Request request = requestsByTarget.get(targetId);
        if (request == null || !request.isExpired()) {
            return;
        }
        removeRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(config.message("expired", "<red>[TPA] 요청이 만료되었습니다."));
        }
    }

    private void clearExpiredOutgoingRequest(UUID requesterId) {
        UUID targetId = targetByRequester.get(requesterId);
        if (targetId == null) {
            return;
        }
        clearExpiredRequest(targetId);
        Request request = requestsByTarget.get(targetId);
        if (request == null || !request.requesterId.equals(requesterId)) {
            targetByRequester.remove(requesterId);
        }
    }

    private void removeIncomingRequest(UUID targetId) {
        Request request = requestsByTarget.get(targetId);
        if (request == null) {
            return;
        }
        removeRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(config.message(
                    "cancelled-target-quit",
                    "<red>[TPA] 대상이 접속 종료하여 TPA 요청이 취소되었습니다."
            ));
        }
    }

    private void removeOutgoingRequest(UUID requesterId, boolean notifyTarget) {
        UUID targetId = targetByRequester.get(requesterId);
        if (targetId == null) {
            return;
        }
        Request request = requestsByTarget.get(targetId);
        if (request == null || !request.requesterId.equals(requesterId)) {
            targetByRequester.remove(requesterId);
            return;
        }
        removeRequest(request);
        if (notifyTarget) {
            Player target = Bukkit.getPlayer(targetId);
            Player requester = Bukkit.getPlayer(requesterId);
            if (target != null && target.isOnline() && requester != null) {
                target.sendMessage(config.message("cancelled-requester-quit",
                        "<gray>[TPA] %requester% 님의 요청이 취소되었습니다.",
                        placeholder("requester", requester.getName())));
            }
        }
    }

    private void removeRequest(Request request) {
        requestsByTarget.remove(request.targetId);
        targetByRequester.remove(request.requesterId);
    }

    private Result ok(String path, String fallback) {
        return new Result(true, config.message(path, fallback));
    }

    private Result fail(String path, String fallback) {
        return new Result(false, config.message(path, fallback));
    }

    public record Result(boolean success, Component message) {
        public void send(Player player) {
            player.sendMessage(message);
        }
    }

    private record Request(UUID requesterId, UUID targetId, long expiresAt) {

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
        }
}
