package org.kkaemok.dongwon.land;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class LandClaim {
    private final String key;
    private final String name;
    private UUID ownerId;
    private String ownerName;
    private final String worldName;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final long createdAt;
    private final Map<UUID, String> members;

    public LandClaim(
            String key,
            String name,
            UUID ownerId,
            String ownerName,
            String worldName,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            long createdAt,
            Map<UUID, String> members
    ) {
        this.key = key;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxZ = Math.max(minZ, maxZ);
        this.createdAt = createdAt;
        this.members = new LinkedHashMap<>(members);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwner(UUID ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        members.remove(ownerId);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Map<UUID, String> getMembers() {
        return members;
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !worldName.equals(location.getWorld().getName())) {
            return false;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(LandClaim other) {
        if (!worldName.equals(other.worldName)) {
            return false;
        }
        return minX <= other.maxX
                && maxX >= other.minX
                && minZ <= other.maxZ
                && maxZ >= other.minZ;
    }

    public boolean isOwner(UUID playerId) {
        return ownerId.equals(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public void addMember(UUID playerId, String playerName) {
        if (!ownerId.equals(playerId)) {
            members.put(playerId, playerName);
        }
    }

    public boolean removeMember(UUID playerId) {
        return members.remove(playerId) != null;
    }
}
