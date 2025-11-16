package com.primemcdirtshop.dirtshop.progression;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerStatsListener implements Listener {

    private final PlayerStatsService statsService;

    public PlayerStatsListener(PlayerStatsService statsService) {
        this.statsService = statsService;
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        int healthValue = (int) Math.max(1, event.getEntity().getMaxHealth());
        statsService.addExperience(killer, StatType.COMBAT, healthValue);
        statsService.addExperience(killer, StatType.HEALTH, healthValue / 2);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        statsService.addExperience(event.getEnchanter(), StatType.MAGIC, event.getExpLevelCost() * 10);
    }

    @EventHandler
    public void onFishing(PlayerFishEvent event) {
        if (event.getCaught() != null) {
            statsService.addExperience(event.getPlayer(), StatType.FISHING, 15);
        }
    }

    @EventHandler
    public void onArchery(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            statsService.addExperience(player, StatType.ARCHERY, 5);
        }
    }

    @EventHandler
    public void onFarming(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.WHEAT || type == Material.CARROTS || type == Material.POTATOES
                || type == Material.BEETROOTS || type == Material.NETHER_WART
                || type == Material.PUMPKIN || type == Material.MELON) {
            statsService.addExperience(event.getPlayer(), StatType.FARMING, 8);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        statsService.apply(event.getPlayer());
    }
}
