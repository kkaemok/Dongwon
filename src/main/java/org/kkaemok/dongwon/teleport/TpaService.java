package org.kkaemok.dongwon.teleport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TpaService {
    private static final long EXPIRE_MILLIS = 60_000L;

    private final Map<UUID, Request> requestsByTarget = new HashMap<>();
    private final Map<UUID, UUID> targetByRequester = new HashMap<>();

    public TpaService() {
    }

    public Result requestTeleport(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            return fail("자기 자신에게 TPA 요청을 보낼 수 없습니다.");
        }

        clearExpiredRequest(target.getUniqueId());
        removeOutgoingRequest(requester.getUniqueId(), false);

        Request existing = requestsByTarget.get(target.getUniqueId());
        if (existing != null && !existing.isExpired()) {
            if (existing.requesterId.equals(requester.getUniqueId())) {
                return fail("이미 해당 플레이어에게 TPA 요청을 보냈습니다.");
            }
            return fail("해당 플레이어는 다른 TPA 요청을 처리 중입니다.");
        }

        Request request = new Request(requester.getUniqueId(), target.getUniqueId(), System.currentTimeMillis() + EXPIRE_MILLIS);
        requestsByTarget.put(target.getUniqueId(), request);
        targetByRequester.put(requester.getUniqueId(), target.getUniqueId());

        target.sendMessage("§e[TPA] " + requester.getName() + " 님이 당신에게 이동 요청을 보냈습니다.");
        target.sendMessage(Component.text("[수락]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.suggestCommand("/tpa 수락 " + requester.getName()))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text("[거절]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.suggestCommand("/tpa 거절 " + requester.getName()))));
        requester.sendMessage("§a[TPA] " + target.getName() + " 님에게 요청을 보냈습니다. (60초)");
        return ok("요청 전송 완료");
    }

    public Result accept(Player target, String requesterName) {
        clearExpiredRequest(target.getUniqueId());

        Request request = requestsByTarget.get(target.getUniqueId());
        if (request == null) {
            return fail("수락할 TPA 요청이 없습니다.");
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester == null || !requester.isOnline()) {
            removeRequest(request);
            return fail("요청한 플레이어가 접속 중이 아닙니다.");
        }

        if (requesterName != null && !requesterName.isBlank()
                && !requester.getName().equalsIgnoreCase(requesterName.trim())) {
            return fail("해당 플레이어의 요청이 아닙니다.");
        }

        removeRequest(request);
        requester.teleport(target.getLocation());
        requester.sendMessage("§a[TPA] " + target.getName() + " 님에게 이동했습니다.");
        target.sendMessage("§a[TPA] " + requester.getName() + " 님의 요청을 수락했습니다.");
        return ok("TPA 요청을 수락했습니다.");
    }

    public Result deny(Player target, String requesterName) {
        clearExpiredRequest(target.getUniqueId());
        Request request = requestsByTarget.get(target.getUniqueId());
        if (request == null) {
            return fail("거절할 TPA 요청이 없습니다.");
        }

        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requesterName != null && !requesterName.isBlank() && requester != null
                && !requester.getName().equalsIgnoreCase(requesterName.trim())) {
            return fail("해당 플레이어의 요청이 아닙니다.");
        }

        removeRequest(request);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§c[TPA] " + target.getName() + " 님이 요청을 거절했습니다.");
        }
        return ok("TPA 요청을 거절했습니다.");
    }

    public void onQuit(Player player) {
        clearExpiredRequest(player.getUniqueId());
        removeIncomingRequest(player.getUniqueId(), "대상이 접속 종료하여 TPA 요청이 취소되었습니다.");
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
            requester.sendMessage("§c[TPA] 요청이 만료되었습니다.");
        }
    }

    private void removeIncomingRequest(UUID targetId, String messageToRequester) {
        Request request = requestsByTarget.get(targetId);
        if (request == null) {
            return;
        }
        removeRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId);
        if (requester != null && requester.isOnline() && messageToRequester != null && !messageToRequester.isBlank()) {
            requester.sendMessage("§c[TPA] " + messageToRequester);
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
                target.sendMessage("§7[TPA] " + requester.getName() + " 님의 요청이 취소되었습니다.");
            }
        }
    }

    private void removeRequest(Request request) {
        requestsByTarget.remove(request.targetId);
        targetByRequester.remove(request.requesterId);
    }

    private static Result ok(String message) {
        return new Result(true, message);
    }

    private static Result fail(String message) {
        return new Result(false, message);
    }

    public record Result(boolean success, String message) {
        public void send(Player player) {
            player.sendMessage((success ? "§a" : "§c") + message);
        }
    }

    private static final class Request {
        private final UUID requesterId;
        private final UUID targetId;
        private final long expiresAt;

        private Request(UUID requesterId, UUID targetId, long expiresAt) {
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
