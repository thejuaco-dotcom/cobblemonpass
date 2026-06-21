package com.cobblepass.common;

public class Quest {
    public enum Type {
        CAPTURE_POKEMON,
        DEFEAT_POKEMON,
        MINE_BLOCK,
        CRAFT_ITEM,
        EVOLVE_POKEMON,
        TRADE_POKEMON,
        HATCH_EGG
    }

    private String id;
    private String title;
    private String description;
    private Type type;
    private String target;
    private int requiredAmount;
    private int xpReward;
    private String category = "SEASONAL"; // "DAILY", "WEEKLY", "SEASONAL"

    public Quest() {
    }

    public Quest(String id, String title, String description, Type type, String target, int requiredAmount, int xpReward) {
        this(id, title, description, type, target, requiredAmount, xpReward, "SEASONAL");
    }

    public Quest(String id, String title, String description, Type type, String target, int requiredAmount, int xpReward, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.target = target;
        this.requiredAmount = requiredAmount;
        this.xpReward = xpReward;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public void setRequiredAmount(int requiredAmount) {
        this.requiredAmount = requiredAmount;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public String getCategory() {
        return category == null ? "SEASONAL" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
