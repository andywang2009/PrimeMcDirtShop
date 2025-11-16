package com.primemcdirtshop.dirtshop.stats;

import com.primemcdirtshop.dirtshop.armory.ArmoryService;
import org.bukkit.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsService {

    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final Map<UUID, Double> magicPool = new HashMap<>();
    private final ArmoryService armoryService;
    private BukkitTask regenTask;

    public PlayerStatsService(JavaPlugin plugin, FileConfiguration config, ArmoryService armoryService) {
        this.plugin = plugin;
        this.config = config;
        this.armoryService = armoryService;
        this.file = new File(plugin.getDataFolder(), "player-stats.yml");
        load();
        startMagicRegen();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerStats value = new PlayerStats();
                value.addAgricultureExp(yaml.getLong(key + ".agriculture", 0));
                value.addCombatExp(yaml.getLong(key + ".combat", 0));
                value.addFishingExp(yaml.getLong(key + ".fishing", 0));
                value.addArcheryExp(yaml.getLong(key + ".archery", 0));
                value.addMagicExp(yaml.getLong(key + ".magic", 0));
                value.addDefenseExp(yaml.getLong(key + ".defense", 0));
                stats.put(uuid, value);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        stats.forEach((uuid, value) -> {
            yaml.set(uuid + ".agriculture", value.getAgricultureExp());
            yaml.set(uuid + ".combat", value.getCombatExp());
            yaml.set(uuid + ".fishing", value.getFishingExp());
            yaml.set(uuid + ".archery", value.getArcheryExp());
            yaml.set(uuid + ".magic", value.getMagicExp());
            yaml.set(uuid + ".defense", value.getDefenseExp());
        });
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to save player stats: " + e.getMessage());
        }
    }

    public PlayerStats get(UUID uuid) {
        return stats.computeIfAbsent(uuid, ignored -> new PlayerStats());
    }

    public PlayerStats getStats(UUID uuid) {
        return get(uuid);
    }

    public void addAgricultureExp(Player player, long amount) {
        get(player.getUniqueId()).addAgricultureExp(amount);
        applyAttributes(player);
    }

    public void addCombatExp(Player player, long amount) {
        get(player.getUniqueId()).addCombatExp(amount);
        applyAttributes(player);
    }

    public void addFishingExp(Player player, long amount) {
        get(player.getUniqueId()).addFishingExp(amount);
    }

    public void addArcheryExp(Player player, long amount) {
        get(player.getUniqueId()).addArcheryExp(amount);
    }

    public void addMagicExp(Player player, long amount) {
        get(player.getUniqueId()).addMagicExp(amount);
        refillMagic(player, amount * 0.05d);
    }

    public void addDefenseExp(Player player, long amount) {
        get(player.getUniqueId()).addDefenseExp(amount);
        applyAttributes(player);
    }

    public void applyAttributes(Player player) {
        double baseHealth = config.getDouble("stats.base.health", 20d);
        double perLevel = config.getDouble("stats.per-level.health", 2d);
        double healthLevel = 1 + getHealthLevel(player.getUniqueId());
        double maxHealth = baseHealth + (healthLevel * perLevel) + armoryService.getHealthBonus(player);
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }
        }

        double baseDefense = config.getDouble("stats.base.defense", 0d);
        double defenseLevel = getDefenseLevel(player.getUniqueId());
        double armorBonus = baseDefense + (defenseLevel * config.getDouble("stats.per-level.defense", 1d)) + armoryService.getDefenseBonus(player);
        if (player.getAttribute(Attribute.GENERIC_ARMOR) != null) {
            player.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armorBonus);
        }
        double magicCapacity = getMaxMagic(player);
        refillMagic(player, magicCapacity);
    }

    public double getMaxMagic(Player player) {
        PlayerStats value = get(player.getUniqueId());
        double baseMagic = config.getDouble("stats.base.magic", 50d);
        double perLevel = config.getDouble("stats.per-level.magic", 10d);
        double magicLevel = getMagicLevel(player.getUniqueId());
        return baseMagic + (magicLevel * perLevel) + armoryService.getMagicBonus(player);
    }

    public double getHealthLevel(UUID uuid) {
        PlayerStats value = get(uuid);
        return value.getCombatExp() / Math.max(1d, config.getDouble("stats.xp-per-level.health", 500d));
    }

    public double getDefenseLevel(UUID uuid) {
        PlayerStats value = get(uuid);
        return value.getDefenseExp() / Math.max(1d, config.getDouble("stats.xp-per-level.defense", 400d));
    }

    public double getMagicLevel(UUID uuid) {
        PlayerStats value = get(uuid);
        return value.getMagicExp() / Math.max(1d, config.getDouble("stats.xp-per-level.magic", 350d));
    }

    public boolean consumeMagic(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        double current = magicPool.computeIfAbsent(player.getUniqueId(), ignored -> getMaxMagic(player));
        if (current < amount) {
            return false;
        }
        magicPool.put(player.getUniqueId(), current - amount);
        return true;
    }

    public double getCurrentMagic(Player player) {
        return magicPool.computeIfAbsent(player.getUniqueId(), ignored -> getMaxMagic(player));
    }

    private void refillMagic(Player player, double amount) {
        double current = getCurrentMagic(player);
        double max = getMaxMagic(player);
        magicPool.put(player.getUniqueId(), Math.min(max, current + amount));
    }

    private void startMagicRegen() {
        regenTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : stats.keySet()) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    refillMagic(player, config.getDouble("stats.magic-regen", 1.5d));
                }
            }
        }, 100L, 100L);
    }

    public void shutdown() {
        if (regenTask != null) {
            regenTask.cancel();
        }
        save();
    }
}
