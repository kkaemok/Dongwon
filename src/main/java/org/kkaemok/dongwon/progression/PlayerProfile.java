package org.kkaemok.dongwon.progression;

public final class PlayerProfile {
    private static final String DEFAULT_GUILD_NAME = "미가입";

    private long can;
    private long silverCan;
    private long goldenCan;
    private long masteryExp;
    private int masteryLevel;
    private MasterySpecialization specialization = MasterySpecialization.NONE;
    private String guildName = DEFAULT_GUILD_NAME;

    public long getCan() {
        return can;
    }

    public void setCan(long can) {
        this.can = Math.max(0L, can);
    }

    public void addCan(long amount) {
        can += Math.max(0L, amount);
    }

    public long getSilverCan() {
        return silverCan;
    }

    public void setSilverCan(long silverCan) {
        this.silverCan = Math.max(0L, silverCan);
    }

    public void addSilverCan(long amount) {
        silverCan += Math.max(0L, amount);
    }

    public long getGoldenCan() {
        return goldenCan;
    }

    public void setGoldenCan(long goldenCan) {
        this.goldenCan = Math.max(0L, goldenCan);
    }

    public long getMasteryExp() {
        return masteryExp;
    }

    public void addMasteryExp(long amount) {
        masteryExp += Math.max(0L, amount);
    }

    public int getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(int masteryLevel) {
        this.masteryLevel = Math.max(0, masteryLevel);
    }

    public MasterySpecialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(MasterySpecialization specialization) {
        this.specialization = specialization == null ? MasterySpecialization.NONE : specialization;
    }

    public String getGuildName() {
        return guildName;
    }

    public void setGuildName(String guildName) {
        if (guildName == null || guildName.isBlank()) {
            this.guildName = DEFAULT_GUILD_NAME;
            return;
        }
        this.guildName = guildName;
    }
}
