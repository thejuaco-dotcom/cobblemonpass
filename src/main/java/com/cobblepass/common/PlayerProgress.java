package com.cobblepass.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProgress {
    public static class ClaimStatus {
        private boolean free;
        private boolean premium;

        public ClaimStatus() {
        }

        public ClaimStatus(boolean free, boolean premium) {
            this.free = free;
            this.premium = premium;
        }

        public boolean isFree() {
            return free;
        }

        public void setFree(boolean free) {
            this.free = free;
        }

        public boolean isPremium() {
            return premium;
        }

        public void setPremium(boolean premium) {
            this.premium = premium;
        }
    }

    private UUID uuid;
    private int level = 0;
    private int currentXp = 0;
    private boolean isPremium = false;
    private String lastResetDate = "";
    private long lastResetWeek = 0;
    private Map<String, Integer> questProgress = new HashMap<>();
    private Map<Integer, ClaimStatus> claimedRewards = new HashMap<>();

    public PlayerProgress() {
    }

    public PlayerProgress(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCurrentXp() {
        return currentXp;
    }

    public void setCurrentXp(int currentXp) {
        this.currentXp = currentXp;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public Map<String, Integer> getQuestProgress() {
        return questProgress;
    }

    public void setQuestProgress(Map<String, Integer> questProgress) {
        this.questProgress = questProgress;
    }

    public Map<Integer, ClaimStatus> getClaimedRewards() {
        return claimedRewards;
    }

    public void setClaimedRewards(Map<Integer, ClaimStatus> claimedRewards) {
        this.claimedRewards = claimedRewards;
    }

    // Helper methods
    public int getQuestProgressCount(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public void incrementQuestProgress(String questId, int amount, int maxAmount) {
        int current = getQuestProgressCount(questId);
        int next = Math.min(current + amount, maxAmount);
        questProgress.put(questId, next);
    }

    public boolean isRewardClaimed(int lvl, boolean premiumReward) {
        ClaimStatus status = claimedRewards.get(lvl);
        if (status == null) {
            return false;
        }
        return premiumReward ? status.isPremium() : status.isFree();
    }

    public void setRewardClaimed(int lvl, boolean premiumReward, boolean claimed) {
        ClaimStatus status = claimedRewards.computeIfAbsent(lvl, k -> new ClaimStatus(false, false));
        if (premiumReward) {
            status.setPremium(claimed);
        } else {
            status.setFree(claimed);
        }
    }

    public String getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(String lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    public long getLastResetWeek() {
        return lastResetWeek;
    }

    public void setLastResetWeek(long lastResetWeek) {
        this.lastResetWeek = lastResetWeek;
    }

    public boolean checkAndResetQuests(java.util.List<String> dailyQuestIds, java.util.List<String> weeklyQuestIds) {
        boolean changed = false;

        // 1. Daily Reset Check
        String today = java.time.LocalDate.now().toString();
        if (!today.equals(lastResetDate)) {
            for (String qId : dailyQuestIds) {
                this.questProgress.remove(qId);
            }
            this.lastResetDate = today;
            changed = true;
        }

        // 2. Weekly Reset Check
        long currentWeek = java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toEpochDay();
        if (currentWeek != lastResetWeek) {
            for (String qId : weeklyQuestIds) {
                this.questProgress.remove(qId);
            }
            this.lastResetWeek = currentWeek;
            changed = true;
        }

        return changed;
    }

    private java.util.List<String> pinnedQuests = new java.util.ArrayList<>();

    public java.util.List<String> getPinnedQuests() {
        if (pinnedQuests == null) {
            pinnedQuests = new java.util.ArrayList<>();
        }
        return pinnedQuests;
    }

    public void setPinnedQuests(java.util.List<String> pinnedQuests) {
        this.pinnedQuests = pinnedQuests;
    }

    public void togglePinQuest(String questId) {
        if (getPinnedQuests().contains(questId)) {
            pinnedQuests.remove(questId);
        } else {
            if (pinnedQuests.size() < 3) {
                pinnedQuests.add(questId);
            }
        }
    }
}
