package com.primemcdirtshop.dirtshop.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataRepository {

    private final JavaPlugin plugin;
    private final File storageFile;
    private final Map<UUID, Long> balances = new HashMap<>();

    public PlayerDataRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for balances");
        }
        this.storageFile = new File(plugin.getDataFolder(), "player-balances.yml");
        load();
    }

    private void load() {
        if (!storageFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long balance = yaml.getLong(key);
                balances.put(uuid, balance);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid UUID in player-balances.yml: " + key);
            }
        }
    }

    public synchronized long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0L);
    }

    public synchronized void setBalance(UUID uuid, long balance) {
        balances.put(uuid, balance);
    }

    public synchronized void addBalance(UUID uuid, long delta) {
        balances.put(uuid, getBalance(uuid) + delta);
    }

    public synchronized void flush() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            yaml.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            yaml.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player balances: " + e.getMessage());
        }
    }
}
