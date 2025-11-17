package com.primemcdirtshop.dirtshop.arsenal;

import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.List;

public record ArmorDefinition(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        double defenseBonus,
        Particle particle
) {
}
