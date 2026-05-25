package org.kkaemok.dongwon.party;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class Party {
    private UUID leaderId;
    private final Set<UUID> members = new LinkedHashSet<>();

    Party(UUID leaderId) {
        this.leaderId = leaderId;
        this.members.add(leaderId);
    }

    UUID getLeaderId() {
        return leaderId;
    }

    void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
        this.members.add(leaderId);
    }

    Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    void addMember(UUID playerId) {
        members.add(playerId);
    }

    void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    boolean isEmpty() {
        return members.isEmpty();
    }
}
