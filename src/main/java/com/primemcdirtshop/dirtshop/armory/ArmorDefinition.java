package com.primemcdirtshop.dirtshop.armory;

import org.bukkit.Material;

import java.util.List;

public class ArmorDefinition {

    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final double healthBonus;
    private final double defenseBonus;
    private final double magicBonus;
    private final String particle;

    public ArmorDefinition(String id, Material material, String displayName, List<String> lore, double healthBonus, double defenseBonus, double magicBonus, String particle) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.healthBonus = healthBonus;
        this.defenseBonus = defenseBonus;
        this.magicBonus = magicBonus;
        this.particle = particle;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public double getHealthBonus() {
        return healthBonus;
    }

    public double getDefenseBonus() {
        return defenseBonus;
    }

    public double getMagicBonus() {
        return magicBonus;
    }

    public String getParticle() {
        return particle;
    }
}
