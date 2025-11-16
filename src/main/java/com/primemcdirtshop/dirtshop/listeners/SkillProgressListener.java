package com.primemcdirtshop.dirtshop.listeners;

import com.primemcdirtshop.dirtshop.stats.PlayerStatsService;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class SkillProgressListener implements Listener {

    private final PlayerStatsService statsService;

    public SkillProgressListener(PlayerStatsService statsService) {
        this.statsService = statsService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.FARMLAND || type.name().contains("CROP")) {
            statsService.addAgricultureExp(event.getPlayer(), 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            statsService.addCombatExp(killer, Math.max(2, event.getDroppedExp()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            statsService.addFishingExp(event.getPlayer(), 3);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            statsService.addArcheryExp(player, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        statsService.addMagicExp(event.getEnchanter(), Math.max(5, event.getExpLevelCost()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && event.getFinalDamage() > 0) {
            statsService.addDefenseExp(player, (long) Math.ceil(event.getFinalDamage()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        statsService.applyAttributes(event.getPlayer());
    }
}
