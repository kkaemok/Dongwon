package org.kkaemok.dongwon.guild;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Guild {
    private final String id;
    private String name;
    private String colorCode;
    private final UUID leaderId;
    private final String luckPermsGroupName;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Guild(String id, String name, String colorCode, UUID leaderId, String luckPermsGroupName) {
        this.id = id;
        this.name = name;
        this.colorCode = colorCode;
        this.leaderId = leaderId;
        this.luckPermsGroupName = luckPermsGroupName;
        this.members.add(leaderId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public String getLuckPermsGroupName() {
        return luckPermsGroupName;
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public String getColoredName() {
        return colorCode + name + "§r";
    }

    public String getSuffixValue() {
        return " " + getColoredName();
    }
}
