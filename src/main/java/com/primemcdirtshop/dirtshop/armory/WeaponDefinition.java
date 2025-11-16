package com.primemcdirtshop.dirtshop.armory;

import org.bukkit.Material;

import java.util.List;

public class WeaponDefinition {

    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final double damageBonus;
    private final WeaponSkill skill;

    public WeaponDefinition(String id, Material material, String displayName, List<String> lore, double damageBonus, WeaponSkill skill) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.damageBonus = damageBonus;
        this.skill = skill;
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

    public double getDamageBonus() {
        return damageBonus;
    }

    public WeaponSkill getSkill() {
        return skill;
    }
}
