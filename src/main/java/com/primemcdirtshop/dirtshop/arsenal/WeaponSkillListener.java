package com.primemcdirtshop.dirtshop.arsenal;

import com.primemcdirtshop.dirtshop.progression.PlayerStatsService;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;

import java.util.List;

public class WeaponSkillListener implements Listener {

    private final WeaponService weaponService;
    private final PlayerStatsService statsService;

    public WeaponSkillListener(WeaponService weaponService) {
        this.weaponService = weaponService;
        this.statsService = weaponService.getStatsService();
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            weaponService.matchWeapon(attacker.getInventory().getItemInMainHand()).ifPresent(def -> {
                event.setDamage(event.getDamage() + def.bonusDamage());
                if (def.particle() != null) {
                    attacker.getWorld().spawnParticle(def.particle(), event.getEntity().getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.01);
                }
            });
        }
        if (event.getEntity() instanceof Player defender) {
            weaponService.matchArmor(defender.getInventory().getChestplate()).ifPresent(def -> {
                event.setDamage(Math.max(0, event.getDamage() - def.defenseBonus() - statsService.getDefenseBonus(defender)));
                if (def.particle() != null) {
                    defender.getWorld().spawnParticle(def.particle(), defender.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
                }
            });
        }
    }

    @EventHandler
    public void onSkill(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        weaponService.matchWeapon(player.getInventory().getItemInMainHand()).ifPresent(def -> {
            switch (def.skill().toUpperCase()) {
                case "WHIRLWIND" -> activateWhirlwind(player, def);
                case "ARCANE_BURST" -> activateArcaneBurst(player, def);
                case "SNIPER_FOCUS" -> activateSniperFocus(player, def);
                default -> {
                }
            }
        });
    }

    private void activateWhirlwind(Player player, WeaponDefinition def) {
        if (!statsService.consumeMagic(player, def.magicCost())) {
            return;
        }
        List<LivingEntity> victims = player.getNearbyEntities(5, 2, 5).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity != player)
                .toList();
        victims.forEach(entity -> entity.damage(def.bonusDamage() + 2, player));
        player.getWorld().spawnParticle(def.particle() != null ? def.particle() : Particle.CRIT, player.getLocation(), 40, 1.2, 0.2, 1.2, 0.05);
    }

    private void activateArcaneBurst(Player player, WeaponDefinition def) {
        if (!statsService.consumeMagic(player, def.magicCost())) {
            return;
        }
        player.getWorld().spawnParticle(def.particle() != null ? def.particle() : Particle.END_ROD,
                player.getEyeLocation(), 20, 0.2, 0.2, 0.2, 0.01);
        RayTraceResult result = player.rayTraceEntities(8);
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.damage(def.bonusDamage() + 4, player);
        }
    }

    private void activateSniperFocus(Player player, WeaponDefinition def) {
        statsService.addExperience(player, com.primemcdirtshop.dirtshop.progression.StatType.ARCHERY, 10);
        player.getWorld().spawnParticle(def.particle() != null ? def.particle() : Particle.CRIT_MAGIC,
                player.getLocation().add(0, 1.8, 0), 10, 0.1, 0.2, 0.1, 0.01);
    }
}
