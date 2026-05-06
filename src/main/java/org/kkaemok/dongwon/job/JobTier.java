package org.kkaemok.dongwon.job;

public enum JobTier {
    COMMON("일반"),
    UNCOMMON("희귀"),
    RARE("레어"),
    EPIC("에픽"),
    LEGENDARY("전설"),
    MYTHIC("신화");

    private final String displayName;

    JobTier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
