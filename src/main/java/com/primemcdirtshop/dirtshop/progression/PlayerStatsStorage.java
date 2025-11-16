package com.primemcdirtshop.dirtshop.progression;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();

    public PlayerStatsStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-stats.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerStats profile = new PlayerStats();
                ConfigurationSection section = yaml.getConfigurationSection(key);
                if (section != null) {
                    for (StatType type : StatType.values()) {
                        profile.setExperience(type, section.getInt(type.getKey(), 0));
                    }
                }
                stats.put(uuid, profile);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid UUID in player-stats.yml: " + key);
            }
        }
    }

    public PlayerStats get(UUID uuid) {
        return stats.computeIfAbsent(uuid, unused -> new PlayerStats());
    }

    public void set(UUID uuid, PlayerStats profile) {
        stats.put(uuid, profile);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        stats.forEach((uuid, profile) -> {
            for (StatType type : StatType.values()) {
                yaml.set(uuid.toString() + "." + type.getKey(), profile.getExperience(type));
            }
        });
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player stats: " + e.getMessage());
        }
    }

    public long getTotalWealthMirror() {
        return stats.values().stream()
                .mapToLong(profile -> profile.getExperience(StatType.MAGIC)
                        + profile.getExperience(StatType.COMBAT)
                        + profile.getExperience(StatType.FARMING))
                .sum();
    }
}
