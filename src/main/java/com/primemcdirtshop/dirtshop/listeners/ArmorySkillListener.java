package com.primemcdirtshop.dirtshop.listeners;

import com.primemcdirtshop.dirtshop.armory.ArmorDefinition;
import com.primemcdirtshop.dirtshop.armory.ArmoryService;
import com.primemcdirtshop.dirtshop.armory.WeaponDefinition;
import com.primemcdirtshop.dirtshop.armory.WeaponSkill;
import com.primemcdirtshop.dirtshop.armory.WeaponSkillType;
import com.primemcdirtshop.dirtshop.stats.PlayerStatsService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ArmorySkillListener implements Listener {

    private final ArmoryService armoryService;
    private final PlayerStatsService statsService;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> lastParticle = new HashMap<>();

    public ArmorySkillListener(ArmoryService armoryService, PlayerStatsService statsService) {
        this.armoryService = armoryService;
        this.statsService = statsService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeaponUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack == null) {
            return;
        }
        Player player = event.getPlayer();
        Optional<WeaponDefinition> definition = armoryService.resolveWeapon(stack);
        if (definition.isEmpty()) {
            return;
        }
        WeaponSkill skill = definition.get().getSkill();
        if (skill == null) {
            return;
        }
        if (!statsService.consumeMagic(player, skill.getMagicCost())) {
            player.sendActionBar(ChatColor.RED + "魔法不足，无法施放技能！");
            return;
        }
        if (isOnCooldown(player.getUniqueId(), definition.get().getId())) {
            player.sendActionBar(ChatColor.YELLOW + "技能冷却中");
            return;
        }
        triggerSkill(player, definition.get(), skill);
    }

    private void triggerSkill(Player player, WeaponDefinition definition, WeaponSkill skill) {
        putCooldown(player.getUniqueId(), definition.getId(), skill.getCooldownSeconds());
        WeaponSkillType type = skill.getType();
        switch (type) {
            case EARTHQUAKE -> performEarthquake(player, skill);
            case ARCANE_NOVA -> performNova(player, skill);
            case HEALING_CHANT -> performHealing(player, skill);
        }
        statsService.addCombatExp(player, (long) (skill.getDamage() * 4));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
    }

    private void performEarthquake(Player player, WeaponSkill skill) {
        Location center = player.getLocation();
        spawnParticles(center, skill.getParticle(), skill.getRadius());
        for (Entity entity : player.getNearbyEntities(skill.getRadius(), skill.getRadius(), skill.getRadius())) {
            if (entity instanceof LivingEntity living && entity != player) {
                living.damage(skill.getDamage(), player);
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1, true, true, true));
            }
        }
    }

    private void performNova(Player player, WeaponSkill skill) {
        spawnParticles(player.getLocation(), skill.getParticle(), skill.getRadius());
        for (Entity entity : player.getNearbyEntities(skill.getRadius(), skill.getRadius(), skill.getRadius())) {
            if (entity instanceof LivingEntity living && entity != player) {
                living.damage(skill.getDamage() / 2, player);
                living.setVelocity(living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.8));
            }
        }
    }

    private void performHealing(Player player, WeaponSkill skill) {
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null ? player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20d;
        player.setHealth(Math.min(maxHealth, player.getHealth() + skill.getDamage()));
        for (Entity entity : player.getNearbyEntities(skill.getRadius(), skill.getRadius(), skill.getRadius())) {
            if (entity instanceof Player nearby) {
                double nearMax = nearby.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null ? nearby.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20d;
                nearby.setHealth(Math.min(nearMax, nearby.getHealth() + skill.getDamage() / 2));
            }
        }
        spawnParticles(player.getLocation(), skill.getParticle(), skill.getRadius());
    }

    private boolean isOnCooldown(UUID uuid, String id) {
        Map<String, Long> entries = cooldowns.computeIfAbsent(uuid, ignored -> new HashMap<>());
        long now = System.currentTimeMillis();
        Long expiry = entries.get(id);
        return expiry != null && expiry > now;
    }

    private void putCooldown(UUID uuid, String id, int cooldownSeconds) {
        cooldowns.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(id, System.currentTimeMillis() + cooldownSeconds * 1000L);
    }

    private void spawnParticles(Location location, String particleName, double radius) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            location.getWorld().spawnParticle(particle, location, 50, radius / 2, 0.2, radius / 2, 0.01);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Optional<WeaponDefinition> definition = armoryService.resolveWeapon(player.getInventory().getItemInMainHand());
        definition.ifPresent(weaponDefinition -> event.setDamage(event.getDamage() + weaponDefinition.getDamageBonus()));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getTo().distanceSquared(event.getFrom()) == 0) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long last = lastParticle.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 1500L) {
            return;
        }
        lastParticle.put(player.getUniqueId(), now);
        for (ItemStack stack : player.getInventory().getArmorContents()) {
            Optional<ArmorDefinition> definition = armoryService.resolveArmor(stack);
            if (definition.isPresent()) {
                spawnParticles(player.getLocation().add(0, 1, 0), definition.get().getParticle(), 0.5);
                break;
            }
        }
    }
}
