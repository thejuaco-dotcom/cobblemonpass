package com.cobblepass.common;

public class Reward {
    public enum Type {
        ITEM,
        COMMAND,
        POKEMON
    }

    public static class Action {
        private Type type;
        private String value;
        private int amount;
        private String nbt;

        @com.google.gson.annotations.SerializedName("$template")
        private String templateRef;

        public Action() {
        }

        public Action(Type type, String value, int amount) {
            this(type, value, amount, null);
        }

        public Action(Type type, String value, int amount, String nbt) {
            this.type = type;
            this.value = value;
            this.amount = amount;
            this.nbt = nbt;
        }

        public String getTemplateRef() {
            return templateRef;
        }

        public void setTemplateRef(String templateRef) {
            this.templateRef = templateRef;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getNbt() {
            return nbt;
        }

        public void setNbt(String nbt) {
            this.nbt = nbt;
        }
    }

    private int level;
    private int xpRequired;
    private Action freeReward;
    private Action premiumReward;

    public Reward() {
    }

    public Reward(int level, int xpRequired, Action freeReward, Action premiumReward) {
        this.level = level;
        this.xpRequired = xpRequired;
        this.freeReward = freeReward;
        this.premiumReward = premiumReward;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXpRequired() {
        return xpRequired;
    }

    public void setXpRequired(int xpRequired) {
        this.xpRequired = xpRequired;
    }

    public Action getFreeReward() {
        return freeReward;
    }

    public void setFreeReward(Action freeReward) {
        this.freeReward = freeReward;
    }

    public Action getPremiumReward() {
        return premiumReward;
    }

    public void setPremiumReward(Action premiumReward) {
        this.premiumReward = premiumReward;
    }
}
