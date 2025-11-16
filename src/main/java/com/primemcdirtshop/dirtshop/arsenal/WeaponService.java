package com.primemcdirtshop.dirtshop.arsenal;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.progression.PlayerStatsService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class WeaponService {

    private final PluginConfiguration configuration;
    private final PlayerStatsService statsService;
    private final Map<String, WeaponDefinition> weapons = new HashMap<>();
    private final Map<String, ArmorDefinition> armors = new HashMap<>();

    public WeaponService(PluginConfiguration configuration, PlayerStatsService statsService) {
        this.configuration = configuration;
        this.statsService = statsService;
        reload();
    }

    public void reload() {
        weapons.clear();
        armors.clear();
        configuration.getWeaponDefinitions().forEach(def -> weapons.put(def.id(), def));
        configuration.getArmorDefinitions().forEach(def -> armors.put(def.id(), def));
    }

    public Collection<WeaponDefinition> getWeapons() {
        return Collections.unmodifiableCollection(weapons.values());
    }

    public Collection<ArmorDefinition> getArmors() {
        return Collections.unmodifiableCollection(armors.values());
    }

    public Optional<WeaponDefinition> matchWeapon(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        return weapons.values().stream()
                .filter(def -> def.material() == stack.getType())
                .filter(def -> {
                    ItemMeta meta = stack.getItemMeta();
                    if (meta == null || !meta.hasDisplayName()) {
                        return false;
                    }
                    String actual = ChatColor.stripColor(meta.getDisplayName());
                    String expected = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', def.displayName()));
                    return actual.equalsIgnoreCase(expected);
                })
                .findFirst();
    }

    public Optional<ArmorDefinition> matchArmor(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        return armors.values().stream()
                .filter(def -> def.material() == stack.getType())
                .findFirst();
    }

    public boolean giveWeapon(Player player, String id) {
        WeaponDefinition definition = weapons.get(id.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return false;
        }
        player.getInventory().addItem(createWeaponItem(definition));
        return true;
    }

    public boolean giveArmor(Player player, String id) {
        ArmorDefinition definition = armors.get(id.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return false;
        }
        player.getInventory().addItem(createArmorItem(definition));
        return true;
    }

    private ItemStack createWeaponItem(WeaponDefinition definition) {
        ItemStack stack = new ItemStack(definition.material());
        stack.editMeta(meta -> {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', definition.displayName()));
            if (definition.lore() != null) {
                meta.setLore(definition.lore().stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
            }
        });
        return stack;
    }

    private ItemStack createArmorItem(ArmorDefinition definition) {
        ItemStack stack = new ItemStack(definition.material());
        stack.editMeta(meta -> {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', definition.displayName()));
            if (definition.lore() != null) {
                meta.setLore(definition.lore().stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
            }
        });
        return stack;
    }

    public PlayerStatsService getStatsService() {
        return statsService;
    }
}
