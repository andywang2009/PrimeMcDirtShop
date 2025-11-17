package com.primemcdirtshop.dirtshop.progression;

import java.util.EnumMap;
import java.util.Map;

public class PlayerStats {

    private final Map<StatType, Integer> experience = new EnumMap<>(StatType.class);

    public int getExperience(StatType type) {
        return experience.getOrDefault(type, 0);
    }

    public void setExperience(StatType type, int amount) {
        experience.put(type, Math.max(0, amount));
    }

    public Map<StatType, Integer> asMap() {
        return experience;
    }

    public int getLevel(StatType type) {
        return getExperience(type) / 100;
    }

    public double getHealthBonus() {
        return getLevel(StatType.HEALTH);
    }

    public double getMagicCapacity() {
        return 100 + getLevel(StatType.MAGIC) * 5.0;
    }

    public double getDefenseBonus() {
        return getLevel(StatType.DEFENSE) * 0.25;
    }
}
