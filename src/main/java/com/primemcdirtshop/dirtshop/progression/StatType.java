package com.primemcdirtshop.dirtshop.progression;

/**
 * Supported成长属性和经验类型。
 */
public enum StatType {
    HEALTH("health"),
    MAGIC("magic"),
    DEFENSE("defense"),
    FARMING("farming"),
    COMBAT("combat"),
    FISHING("fishing"),
    ARCHERY("archery");

    private final String key;

    StatType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
