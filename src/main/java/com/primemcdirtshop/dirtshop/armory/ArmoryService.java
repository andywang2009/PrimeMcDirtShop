package com.primemcdirtshop.dirtshop.armory;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArmoryService {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final NamespacedKey weaponKey;
    private final NamespacedKey armorKey;
    private final Map<String, WeaponDefinition> weapons = new HashMap<>();
    private final Map<String, ArmorDefinition> armors = new HashMap<>();

    public ArmoryService(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.weaponKey = new NamespacedKey(plugin, "armory-weapon");
        this.armorKey = new NamespacedKey(plugin, "armory-armor");
        reload();
    }

    public void reload() {
        weapons.clear();
        armors.clear();
        ConfigurationSection section = config.getConfigurationSection("armory.weapons");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection weaponSection = section.getConfigurationSection(key);
                if (weaponSection == null) {
                    continue;
                }
                Material material = Material.matchMaterial(weaponSection.getString("material", "STONE_SWORD"));
                if (material == null) {
                    continue;
                }
                double damage = weaponSection.getDouble("damage-bonus", 3.0d);
                List<String> lore = weaponSection.getStringList("lore");
                String display = weaponSection.getString("display-name", material.name());
                WeaponSkill skill = loadSkill(weaponSection.getConfigurationSection("skill"));
                weapons.put(key.toLowerCase(), new WeaponDefinition(key.toLowerCase(), material, display, lore, damage, skill));
            }
        }
        ConfigurationSection armorSection = config.getConfigurationSection("armory.armors");
        if (armorSection != null) {
            for (String key : armorSection.getKeys(false)) {
                ConfigurationSection defSection = armorSection.getConfigurationSection(key);
                if (defSection == null) {
                    continue;
                }
                Material material = Material.matchMaterial(defSection.getString("material", "LEATHER_CHESTPLATE"));
                if (material == null) {
                    continue;
                }
                String display = defSection.getString("display-name", material.name());
                List<String> lore = defSection.getStringList("lore");
                double health = defSection.getDouble("health-bonus", 0d);
                double defense = defSection.getDouble("defense-bonus", 0d);
                double magic = defSection.getDouble("magic-bonus", 0d);
                String particle = defSection.getString("particle", "HEART");
                armors.put(key.toLowerCase(), new ArmorDefinition(key.toLowerCase(), material, display, lore, health, defense, magic, particle));
            }
        }
    }

    private WeaponSkill loadSkill(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        try {
            WeaponSkillType type = WeaponSkillType.valueOf(section.getString("type", "EARTHQUAKE").toUpperCase());
            double cost = section.getDouble("magic-cost", 15d);
            int cooldown = section.getInt("cooldown", 15);
            double radius = section.getDouble("radius", 5d);
            double damage = section.getDouble("damage", 6d);
            String particle = section.getString("particle", "CRIT");
            return new WeaponSkill(type, cost, cooldown, radius, damage, particle);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Collection<String> getWeaponIds() {
        return Collections.unmodifiableCollection(weapons.keySet());
    }

    public Collection<String> getArmorIds() {
        return Collections.unmodifiableCollection(armors.keySet());
    }

    public Optional<WeaponDefinition> getWeapon(String id) {
        return Optional.ofNullable(weapons.get(id.toLowerCase()));
    }

    public Optional<ArmorDefinition> getArmor(String id) {
        return Optional.ofNullable(armors.get(id.toLowerCase()));
    }

    public ItemStack createWeapon(String id) {
        WeaponDefinition definition = weapons.get(id.toLowerCase());
        if (definition == null) {
            return null;
        }
        ItemStack stack = new ItemStack(definition.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(definition.getDisplayName());
            if (!definition.getLore().isEmpty()) {
                meta.setLore(definition.getLore());
            }
            meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, definition.getId());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack createArmor(String id) {
        ArmorDefinition definition = armors.get(id.toLowerCase());
        if (definition == null) {
            return null;
        }
        ItemStack stack = new ItemStack(definition.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(definition.getDisplayName());
            if (!definition.getLore().isEmpty()) {
                meta.setLore(new ArrayList<>(definition.getLore()));
            }
            meta.getPersistentDataContainer().set(armorKey, PersistentDataType.STRING, definition.getId());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public Optional<WeaponDefinition> resolveWeapon(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(weaponKey, PersistentDataType.STRING);
        if (id != null) {
            return getWeapon(id);
        }
        return weapons.values().stream()
                .filter(def -> def.getMaterial() == stack.getType())
                .findFirst();
    }

    public Optional<ArmorDefinition> resolveArmor(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(armorKey, PersistentDataType.STRING);
        if (id != null) {
            return getArmor(id);
        }
        return armors.values().stream()
                .filter(def -> def.getMaterial() == stack.getType())
                .findFirst();
    }

    public double getHealthBonus(Player player) {
        return getArmorBonus(player, ArmorDefinition::getHealthBonus);
    }

    public double getDefenseBonus(Player player) {
        return getArmorBonus(player, ArmorDefinition::getDefenseBonus);
    }

    public double getMagicBonus(Player player) {
        return getArmorBonus(player, ArmorDefinition::getMagicBonus);
    }

    private double getArmorBonus(Player player, java.util.function.ToDoubleFunction<ArmorDefinition> mapper) {
        ItemStack[] contents = player.getInventory().getArmorContents();
        double total = 0;
        for (ItemStack content : contents) {
            Optional<ArmorDefinition> definition = resolveArmor(content);
            if (definition.isPresent()) {
                total += mapper.applyAsDouble(definition.get());
            }
        }
        return total;
    }
}
