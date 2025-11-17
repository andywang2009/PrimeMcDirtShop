package com.primemcdirtshop.dirtshop.progression;

import org.bukkit.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsService {

    private final PlayerStatsStorage storage;
    private final Map<UUID, Double> magicReserve = new HashMap<>();

    public PlayerStatsService(PlayerStatsStorage storage) {
        this.storage = storage;
    }

    public PlayerStats get(Player player) {
        return storage.get(player.getUniqueId());
    }

    public void addExperience(Player player, StatType type, int amount) {
        if (amount <= 0) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerStats stats = storage.get(uuid);
        stats.setExperience(type, stats.getExperience(type) + amount);
        storage.set(uuid, stats);
        apply(player);
    }

    public void apply(Player player) {
        PlayerStats stats = storage.get(player.getUniqueId());
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20 + stats.getHealthBonus());
        }
        magicReserve.put(player.getUniqueId(), stats.getMagicCapacity());
    }

    public double getMagic(Player player) {
        return magicReserve.getOrDefault(player.getUniqueId(), get(player).getMagicCapacity());
    }

    public boolean consumeMagic(Player player, double amount) {
        double current = getMagic(player);
        if (current < amount) {
            return false;
        }
        magicReserve.put(player.getUniqueId(), current - amount);
        return true;
    }

    public double getDefenseBonus(Player player) {
        return get(player).getDefenseBonus();
    }

    public void save() {
        storage.save();
    }
}
