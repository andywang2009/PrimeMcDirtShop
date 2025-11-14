package com.primemcdirtshop.dirtshop.tools;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ToolModificationService {

    private final PluginConfiguration configuration;

    public ToolModificationService(PluginConfiguration configuration) {
        this.configuration = configuration;
    }

    public void apply(Player player) {
        if (player == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            apply(stack);
        }
    }

    public void apply(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        Optional<ToolModifier> modifierOptional = configuration.getToolModifier(stack.getType());
        if (modifierOptional.isEmpty()) {
            return;
        }
        ToolModifier modifier = modifierOptional.get();
        stack.editMeta(meta -> applyMeta(meta, modifier));
        applyEnchants(stack, modifier);
    }

    private void applyMeta(ItemMeta meta, ToolModifier modifier) {
        if (modifier.displayName() != null && !modifier.displayName().isEmpty()) {
            meta.setDisplayName(color(modifier.displayName()));
        }
        List<String> lore = modifier.lore();
        if (!lore.isEmpty()) {
            meta.setLore(lore.stream().map(this::color).toList());
        }
        meta.setUnbreakable(modifier.unbreakable());
    }

    private void applyEnchants(ItemStack stack, ToolModifier modifier) {
        Map<Enchantment, Integer> enchantments = modifier.enchantments();
        if (enchantments.isEmpty()) {
            return;
        }
        enchantments.forEach((enchantment, level) -> {
            if (enchantment != null && level != null && level > 0) {
                stack.addUnsafeEnchantment(enchantment, level);
            }
        });
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public int calculateDurabilityDamage(ItemStack stack, int baseDamage) {
        if (stack == null || stack.getType() == Material.AIR || baseDamage <= 0) {
            return 0;
        }
        Optional<ToolModifier> modifierOptional = configuration.getToolModifier(stack.getType());
        if (modifierOptional.isEmpty()) {
            return baseDamage;
        }
        ToolModifier modifier = modifierOptional.get();
        if (modifier.unbreakable()) {
            return 0;
        }
        double multiplier = modifier.durabilityMultiplier();
        if (multiplier == 1.0d) {
            return baseDamage;
        }
        if (multiplier <= 0) {
            return 0;
        }
        int damage = (int) Math.round(baseDamage * multiplier);
        return Math.max(damage, 1);
    }

    public boolean shouldApplyOnJoin() {
        return configuration.shouldApplyToolModifiersOnJoin();
    }

    public List<String> describeModifier(Material material) {
        Optional<ToolModifier> modifierOptional = configuration.getToolModifier(material);
        if (modifierOptional.isEmpty()) {
            return List.of();
        }
        ToolModifier modifier = modifierOptional.get();
        String name = modifier.displayName() != null ? color(modifier.displayName()) : material.name().toLowerCase(Locale.ROOT);
        String durability = modifier.unbreakable()
                ? ChatColor.GREEN + "不可损坏"
                : ChatColor.YELLOW + "耐久倍率: " + modifier.durabilityMultiplier();
        return List.of(
                ChatColor.GOLD + name,
                durability
        );
    }

    public Map<Material, ToolModifier> getAllModifiers() {
        return configuration.getToolModifiers();
    }
}
