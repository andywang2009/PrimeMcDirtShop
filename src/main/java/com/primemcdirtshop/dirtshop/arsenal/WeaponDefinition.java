package com.primemcdirtshop.dirtshop.arsenal;

import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.List;

public record WeaponDefinition(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        String skill,
        Particle particle,
        double magicCost,
        double bonusDamage
) {
}
