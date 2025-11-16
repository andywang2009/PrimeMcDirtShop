package com.primemcdirtshop.dirtshop.stats;

public class PlayerStats {

    private long agricultureExp;
    private long combatExp;
    private long fishingExp;
    private long archeryExp;
    private long magicExp;
    private long defenseExp;

    public long getAgricultureExp() {
        return agricultureExp;
    }

    public void addAgricultureExp(long delta) {
        agricultureExp += Math.max(0, delta);
    }

    public long getCombatExp() {
        return combatExp;
    }

    public void addCombatExp(long delta) {
        combatExp += Math.max(0, delta);
    }

    public long getFishingExp() {
        return fishingExp;
    }

    public void addFishingExp(long delta) {
        fishingExp += Math.max(0, delta);
    }

    public long getArcheryExp() {
        return archeryExp;
    }

    public void addArcheryExp(long delta) {
        archeryExp += Math.max(0, delta);
    }

    public long getMagicExp() {
        return magicExp;
    }

    public void addMagicExp(long delta) {
        magicExp += Math.max(0, delta);
    }

    public long getDefenseExp() {
        return defenseExp;
    }

    public void addDefenseExp(long delta) {
        defenseExp += Math.max(0, delta);
    }
}
