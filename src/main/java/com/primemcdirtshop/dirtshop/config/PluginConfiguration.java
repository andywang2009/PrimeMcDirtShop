package com.primemcdirtshop.dirtshop.config;

import com.primemcdirtshop.dirtshop.arsenal.ArmorDefinition;
import com.primemcdirtshop.dirtshop.arsenal.WeaponDefinition;
import com.primemcdirtshop.dirtshop.tools.ToolModifier;
import com.primemcdirtshop.dirtshop.util.ShopTrade;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PluginConfiguration {

    private FileConfiguration config;
    private Map<Material, Integer> blockValues;
    private List<ShopTrade> trades;
    private Map<Material, ToolModifier> toolModifiers;
    private boolean applyToolModifiersOnJoin;
    private WelcomeSettings welcomeSettings;
    private List<WeaponDefinition> weaponDefinitions;
    private List<ArmorDefinition> armorDefinitions;

    public PluginConfiguration(FileConfiguration config) {
        reload(config);
    }

    public synchronized void reload(FileConfiguration config) {
        this.config = config;
        this.blockValues = Collections.unmodifiableMap(loadBlockValues());
        this.trades = Collections.unmodifiableList(loadTrades());
        this.toolModifiers = Collections.unmodifiableMap(loadToolModifiers());
        this.applyToolModifiersOnJoin = config.getBoolean("tool-modifiers.apply-on-join", true);
        this.welcomeSettings = loadWelcomeSettings();
        this.weaponDefinitions = Collections.unmodifiableList(loadWeaponDefinitions());
        this.armorDefinitions = Collections.unmodifiableList(loadArmorDefinitions());
    }

    private Map<Material, Integer> loadBlockValues() {
        ConfigurationSection section = config.getConfigurationSection("dirt-currency.values");
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<Material, Integer> values = new EnumMap<>(Material.class);
        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            Material material = Material.matchMaterial(key);
            if (material != null) {
                values.put(material, section.getInt(key));
            }
        }
        return values;
    }

    private List<ShopTrade> loadTrades() {
        List<Map<?, ?>> rawTrades = config.getMapList("shop.trades");
        if (rawTrades.isEmpty()) {
            return Collections.emptyList();
        }
        List<ShopTrade> parsed = new ArrayList<>();
        for (Map<?, ?> raw : rawTrades) {
            Object itemId = raw.get("item");
            Object amount = raw.get("amount");
            Object cost = raw.get("cost");
            if (!(itemId instanceof String) || !(amount instanceof Number) || !(cost instanceof Number)) {
                continue;
            }
            Material material = Material.matchMaterial((String) itemId);
            if (material == null) {
                continue;
            }
            parsed.add(new ShopTrade(material, ((Number) amount).intValue(), ((Number) cost).intValue()));
        }
        return parsed;
    }

    private Map<Material, ToolModifier> loadToolModifiers() {
        ConfigurationSection section = config.getConfigurationSection("tool-modifiers.items");
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<Material, ToolModifier> modifiers = new EnumMap<>(Material.class);
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                continue;
            }
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            String displayName = itemSection.getString("display-name");
            List<String> lore = itemSection.getStringList("lore");
            boolean unbreakable = itemSection.getBoolean("unbreakable", false);
            double durabilityMultiplier = itemSection.getDouble("durability-multiplier", 1.0d);
            ConfigurationSection enchantmentSection = itemSection.getConfigurationSection("enchantments");
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            if (enchantmentSection != null) {
                for (String enchantKey : enchantmentSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByName(enchantKey.toUpperCase(Locale.ROOT));
                    if (enchantment == null) {
                        continue;
                    }
                    int level = enchantmentSection.getInt(enchantKey, 1);
                    enchantments.put(enchantment, level);
                }
            }
            modifiers.put(material, new ToolModifier(displayName, lore, enchantments, unbreakable, durabilityMultiplier));
        }
        return modifiers;
    }

    private WelcomeSettings loadWelcomeSettings() {
        boolean enabled = config.getBoolean("welcome.enabled", true);
        String title = config.getString("welcome.title");
        String subtitle = config.getString("welcome.subtitle");
        List<String> messages = config.getStringList("welcome.messages");
        return new WelcomeSettings(enabled, title, subtitle, messages);
    }

    public Map<Material, Integer> getBlockValues() {
        return blockValues;
    }

    public boolean isFortuneBonusEnabled() {
        return config.getBoolean("dirt-currency.fortune-bonus", true);
    }

    public List<ShopTrade> getShopTrades() {
        return trades;
    }

    public int getMaxListingsPerPlayer() {
        return config.getInt("market.max-listings-per-player", 5);
    }

    public int getListingExpiryHours() {
        return config.getInt("market.listing-expiry-hours", 168);
    }

    public Optional<ToolModifier> getToolModifier(Material material) {
        return Optional.ofNullable(toolModifiers.get(material));
    }

    public Map<Material, ToolModifier> getToolModifiers() {
        return toolModifiers;
    }

    public boolean shouldApplyToolModifiersOnJoin() {
        return applyToolModifiersOnJoin;
    }

    public WelcomeSettings getWelcomeSettings() {
        return welcomeSettings;
    }

    public List<WeaponDefinition> getWeaponDefinitions() {
        return weaponDefinitions;
    }

    public List<ArmorDefinition> getArmorDefinitions() {
        return armorDefinitions;
    }

    private List<WeaponDefinition> loadWeaponDefinitions() {
        List<Map<?, ?>> raw = config.getMapList("arsenal.weapons");
        List<WeaponDefinition> list = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            Object item = entry.get("item");
            if (!(item instanceof String itemId)) {
                continue;
            }
            Material material = Material.matchMaterial(itemId);
            if (material == null) {
                continue;
            }
            String id = String.valueOf(entry.getOrDefault("id", material.name().toLowerCase(Locale.ROOT)));
            String display = String.valueOf(entry.getOrDefault("display-name", material.name()));
            @SuppressWarnings("unchecked")
            List<String> lore = (List<String>) entry.getOrDefault("lore", Collections.emptyList());
            String skill = String.valueOf(entry.getOrDefault("skill", "NONE"));
            Particle particle = parseParticle(String.valueOf(entry.getOrDefault("particle", Particle.CRIT.name())));
            double magicCost = entry.get("magic-cost") instanceof Number number ? number.doubleValue() : 10d;
            double bonusDamage = entry.get("bonus-damage") instanceof Number damage ? damage.doubleValue() : 3d;
            list.add(new WeaponDefinition(id.toLowerCase(Locale.ROOT), material, display, lore, skill, particle, magicCost, bonusDamage));
        }
        return list;
    }

    private List<ArmorDefinition> loadArmorDefinitions() {
        List<Map<?, ?>> raw = config.getMapList("arsenal.armors");
        List<ArmorDefinition> list = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            Object item = entry.get("item");
            if (!(item instanceof String itemId)) {
                continue;
            }
            Material material = Material.matchMaterial(itemId);
            if (material == null) {
                continue;
            }
            String id = String.valueOf(entry.getOrDefault("id", material.name().toLowerCase(Locale.ROOT)));
            String display = String.valueOf(entry.getOrDefault("display-name", material.name()));
            @SuppressWarnings("unchecked")
            List<String> lore = (List<String>) entry.getOrDefault("lore", Collections.emptyList());
            double defense = entry.get("defense-bonus") instanceof Number number ? number.doubleValue() : 1.5d;
            Particle particle = parseParticle(String.valueOf(entry.getOrDefault("particle", Particle.SPELL_MOB.name())));
            list.add(new ArmorDefinition(id.toLowerCase(Locale.ROOT), material, display, lore, defense, particle));
        }
        return list;
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Particle.CRIT;
        }
    }
}
