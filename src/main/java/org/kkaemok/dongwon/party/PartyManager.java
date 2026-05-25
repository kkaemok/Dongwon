package org.kkaemok.dongwon.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kkaemok.dongwon.text.ConfigText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class PartyManager {
    private final ConfigText text;
    private final Map<UUID, Party> partyByMember = new HashMap<>();
    private final Map<UUID, Invite> invitesByTarget = new HashMap<>();

    public PartyManager(ConfigText text) {
        this.text = text;
    }

    public void create(Player player) {
        UUID playerId = player.getUniqueId();
        if (partyByMember.containsKey(playerId)) {
            text.send(player, "messages.party.already-in-party", "<red>이미 파티에 가입되어 있습니다.");
            return;
        }

        Party party = new Party(playerId);
        partyByMember.put(playerId, party);
        text.send(player, "messages.party.created", "<green>파티를 생성했습니다.");
    }

    public void invite(Player leader, Player target) {
        Party party = partyByMember.get(leader.getUniqueId());
        if (party == null) {
            text.send(leader, "messages.party.not-in-party", "<red>파티에 가입되어 있지 않습니다.");
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            text.send(leader, "messages.party.leader-only", "<red>파티장만 사용할 수 있습니다.");
            return;
        }
        if (target.getUniqueId().equals(leader.getUniqueId())) {
            text.send(leader, "messages.party.cannot-invite-self", "<red>자기 자신은 초대할 수 없습니다.");
            return;
        }
        if (partyByMember.containsKey(target.getUniqueId())) {
            text.send(leader, "messages.party.target-already-in-party", "<red>해당 플레이어는 이미 파티에 가입되어 있습니다.");
            return;
        }

        invitesByTarget.put(target.getUniqueId(), new Invite(party, leader.getUniqueId()));
        text.send(leader, "messages.party.invite-sent", "<green>%player% 님에게 파티 초대를 보냈습니다.",
                placeholder("player", target.getName()));
        text.send(target, "messages.party.invite-received", "<yellow>%player% 님이 파티에 초대했습니다. /파티 수락",
                placeholder("player", leader.getName()));
    }

    public void accept(Player player) {
        Invite invite = invitesByTarget.remove(player.getUniqueId());
        if (invite == null || !partyByMember.containsValue(invite.party())) {
            text.send(player, "messages.party.no-invite", "<red>받은 파티 초대가 없습니다.");
            return;
        }
        if (partyByMember.containsKey(player.getUniqueId())) {
            text.send(player, "messages.party.already-in-party", "<red>이미 파티에 가입되어 있습니다.");
            return;
        }

        Party party = invite.party();
        party.addMember(player.getUniqueId());
        partyByMember.put(player.getUniqueId(), party);
        broadcast(party, "messages.party.joined", "<green>%player% 님이 파티에 참가했습니다.",
                placeholder("player", player.getName()));
    }

    public void deny(Player player) {
        Invite invite = invitesByTarget.remove(player.getUniqueId());
        if (invite == null) {
            text.send(player, "messages.party.no-invite", "<red>받은 파티 초대가 없습니다.");
            return;
        }

        Player leader = Bukkit.getPlayer(invite.leaderId());
        text.send(player, "messages.party.invite-denied", "<yellow>파티 초대를 거절했습니다.");
        if (leader != null && leader.isOnline()) {
            text.send(leader, "messages.party.invite-denied-notify", "<yellow>%player% 님이 파티 초대를 거절했습니다.",
                    placeholder("player", player.getName()));
        }
    }

    public void kick(Player leader, Player target) {
        Party party = partyByMember.get(leader.getUniqueId());
        if (party == null) {
            text.send(leader, "messages.party.not-in-party", "<red>파티에 가입되어 있지 않습니다.");
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            text.send(leader, "messages.party.leader-only", "<red>파티장만 사용할 수 있습니다.");
            return;
        }
        if (target.getUniqueId().equals(leader.getUniqueId())) {
            text.send(leader, "messages.party.cannot-kick-self", "<red>자기 자신은 퇴출할 수 없습니다.");
            return;
        }
        if (partyByMember.get(target.getUniqueId()) != party) {
            text.send(leader, "messages.party.target-not-member", "<red>해당 플레이어는 파티원이 아닙니다.");
            return;
        }

        removeMember(party, target.getUniqueId());
        text.send(target, "messages.party.kicked", "<red>파티에서 퇴출되었습니다.");
        broadcast(party, "messages.party.kick-notify", "<yellow>%player% 님이 파티에서 퇴출되었습니다.",
                placeholder("player", target.getName()));
    }

    public void disband(Player leader) {
        Party party = partyByMember.get(leader.getUniqueId());
        if (party == null) {
            text.send(leader, "messages.party.not-in-party", "<red>파티에 가입되어 있지 않습니다.");
            return;
        }
        if (!party.isLeader(leader.getUniqueId())) {
            text.send(leader, "messages.party.leader-only", "<red>파티장만 사용할 수 있습니다.");
            return;
        }

        broadcast(party, "messages.party.disbanded", "<red>파티가 해제되었습니다.");
        removeParty(party);
    }

    public void leave(Player player) {
        Party party = partyByMember.get(player.getUniqueId());
        if (party == null) {
            text.send(player, "messages.party.not-in-party", "<red>파티에 가입되어 있지 않습니다.");
            return;
        }
        leaveInternal(player, true);
    }

    public void handleQuit(Player player) {
        leaveInternal(player, false);
        invitesByTarget.remove(player.getUniqueId());
    }

    public boolean isSameParty(Player first, Player second) {
        Party party = partyByMember.get(first.getUniqueId());
        return party != null && partyByMember.get(second.getUniqueId()) == party;
    }

    public List<Player> getOnlineMembersExcept(Player player) {
        Party party = partyByMember.get(player.getUniqueId());
        if (party == null) {
            return List.of();
        }

        List<Player> members = new ArrayList<>();
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(player.getUniqueId())) {
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                members.add(member);
            }
        }
        return members;
    }

    public void info(Player player) {
        Party party = partyByMember.get(player.getUniqueId());
        if (party == null) {
            text.send(player, "messages.party.not-in-party", "<red>파티에 가입되어 있지 않습니다.");
            return;
        }

        String leaderName = nameOf(party.getLeaderId());
        String members = party.getMembers().stream()
                .map(this::nameOf)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        text.send(player, "messages.party.info", "<yellow>파티장: %leader% <gray>| <white>파티원: %members%",
                placeholder("leader", leaderName),
                placeholder("members", members));
    }

    public List<String> memberNames(Player player) {
        Party party = partyByMember.get(player.getUniqueId());
        if (party == null) {
            return List.of();
        }
        return party.getMembers().stream()
                .filter(memberId -> !memberId.equals(player.getUniqueId()))
                .map(this::nameOf)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> scoreboardLines(Player player) {
        Party party = partyByMember.get(player.getUniqueId());
        int membersPerLine = Math.max(1, parseInt(
                text.string("scoreboard.members-per-line", "3"),
                3
        ));
        String none = text.string("scoreboard.none", "없음");
        String selfName = text.string("scoreboard.self-name", "나");
        String separator = text.string("scoreboard.separator", ", ");
        String firstLineFormat = text.string("scoreboard.first-line-format", "&7파티원 &f%members%");
        String nextLineFormat = text.string("scoreboard.next-line-format", "&7       &f%members%");

        if (party == null) {
            return List.of(firstLineFormat.replace("%members%", none));
        }

        List<String> names = party.getMembers().stream()
                .map(memberId -> memberId.equals(player.getUniqueId()) ? selfName : nameOf(memberId))
                .toList();
        if (names.isEmpty()) {
            return List.of(firstLineFormat.replace("%members%", none));
        }

        List<String> lines = new ArrayList<>();
        for (int index = 0; index < names.size(); index += membersPerLine) {
            int end = Math.min(index + membersPerLine, names.size());
            String members = String.join(separator, names.subList(index, end));
            String format = index == 0 ? firstLineFormat : nextLineFormat;
            lines.add(format.replace("%members%", members));
        }
        return lines;
    }

    private void leaveInternal(Player player, boolean sendSelfMessage) {
        Party party = partyByMember.get(player.getUniqueId());
        if (party == null) {
            return;
        }

        boolean wasLeader = party.isLeader(player.getUniqueId());
        removeMember(party, player.getUniqueId());
        if (sendSelfMessage) {
            text.send(player, "messages.party.left", "<yellow>파티에서 탈퇴했습니다.");
        }

        if (party.isEmpty()) {
            removeParty(party);
            return;
        }

        if (wasLeader) {
            UUID nextLeader = party.getMembers().iterator().next();
            party.setLeaderId(nextLeader);
            broadcast(party, "messages.party.leader-changed", "<yellow>%player% 님이 새 파티장이 되었습니다.",
                    placeholder("player", nameOf(nextLeader)));
        }

        broadcast(party, "messages.party.leave-notify", "<yellow>%player% 님이 파티를 탈퇴했습니다.",
                placeholder("player", player.getName()));
    }

    private void removeMember(Party party, UUID playerId) {
        party.removeMember(playerId);
        partyByMember.remove(playerId);
    }

    private void removeParty(Party party) {
        for (UUID memberId : List.copyOf(party.getMembers())) {
            partyByMember.remove(memberId);
            party.removeMember(memberId);
        }
        invitesByTarget.entrySet().removeIf(entry -> entry.getValue().party() == party);
    }

    private void broadcast(Party party, String path, String fallback, ConfigText.Placeholder... placeholders) {
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                text.send(member, path, fallback, placeholders);
            }
        }
    }

    private String nameOf(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }
        return Bukkit.getOfflinePlayer(playerId).getName() == null ? playerId.toString() : Bukkit.getOfflinePlayer(playerId).getName();
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private record Invite(Party party, UUID leaderId) {
    }
}
