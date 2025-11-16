package com.primemcdirtshop.dirtshop.tools;

import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

public record ToolModifier(
        String displayName,
        List<String> lore,
        Map<Enchantment, Integer> enchantments,
        boolean unbreakable,
        double durabilityMultiplier
) {
    public ToolModifier {
        if (lore == null) {
            lore = List.of();
        }
        if (enchantments == null) {
            enchantments = Map.of();
        }
    }
}
