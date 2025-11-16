package com.primemcdirtshop.dirtshop.armory;

enum WeaponSkillType {
    EARTHQUAKE,
    ARCANE_NOVA,
    HEALING_CHANT
}

public class WeaponSkill {

    private final WeaponSkillType type;
    private final double magicCost;
    private final int cooldownSeconds;
    private final double radius;
    private final double damage;
    private final String particle;

    public WeaponSkill(WeaponSkillType type, double magicCost, int cooldownSeconds, double radius, double damage, String particle) {
        this.type = type;
        this.magicCost = magicCost;
        this.cooldownSeconds = cooldownSeconds;
        this.radius = radius;
        this.damage = damage;
        this.particle = particle;
    }

    public WeaponSkillType getType() {
        return type;
    }

    public double getMagicCost() {
        return magicCost;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getRadius() {
        return radius;
    }

    public double getDamage() {
        return damage;
    }

    public String getParticle() {
        return particle;
    }
}
