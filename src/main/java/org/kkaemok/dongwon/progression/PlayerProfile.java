package org.kkaemok.dongwon.progression;

import org.kkaemok.dongwon.job.JobType;

public final class PlayerProfile {
    private static final String DEFAULT_GUILD_NAME = "미가입";

    private String playerName = "";
    private JobType jobType = JobType.NONE;
    private long can;
    private long silverCan;
    private long goldenCan;
    private int level;
    private double levelExp;
    private long swordsmanMasteryExp;
    private int swordsmanMasteryLevel;
    private MasterySpecialization swordsmanSpecialization = MasterySpecialization.NONE;
    private long fishermanMasteryExp;
    private String guildName = DEFAULT_GUILD_NAME;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName == null ? "" : playerName;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType == null ? JobType.NONE : jobType;
    }

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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.clamp(level, 0, 50);
    }

    public double getLevelExp() {
        return levelExp;
    }

    public void setLevelExp(double levelExp) {
        this.levelExp = Math.max(0.0D, levelExp);
    }

    public long getSwordsmanMasteryExp() {
        return swordsmanMasteryExp;
    }

    public void addSwordsmanMasteryExp(long amount) {
        swordsmanMasteryExp += Math.max(0L, amount);
    }

    public int getSwordsmanMasteryLevel() {
        return swordsmanMasteryLevel;
    }

    public void setSwordsmanMasteryLevel(int swordsmanMasteryLevel) {
        this.swordsmanMasteryLevel = Math.max(0, swordsmanMasteryLevel);
    }

    public MasterySpecialization getSwordsmanSpecialization() {
        return swordsmanSpecialization;
    }

    public void setSwordsmanSpecialization(MasterySpecialization specialization) {
        this.swordsmanSpecialization = specialization == null ? MasterySpecialization.NONE : specialization;
    }

    public long getFishermanMasteryExp() {
        return fishermanMasteryExp;
    }

    public void addFishermanMasteryExp(long amount) {
        fishermanMasteryExp += Math.max(0L, amount);
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
