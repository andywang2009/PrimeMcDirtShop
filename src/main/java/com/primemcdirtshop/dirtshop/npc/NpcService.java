package com.primemcdirtshop.dirtshop.npc;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.economy.DirtEconomyService;
import com.primemcdirtshop.dirtshop.util.ShopService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class NpcService {

    private static final String NPC_TAG = "PrimeMcDirtShopNPC";
    private static final String NPC_ID_PREFIX = NPC_TAG + ":";

    private final JavaPlugin plugin;
    private final PluginConfiguration configuration;
    private final ShopService shopService;
    private final DirtEconomyService economyService;

    private final Map<String, NpcDefinition> definitions = new HashMap<>();
    private final Map<UUID, String> activeNpcIds = new HashMap<>();

    public NpcService(JavaPlugin plugin,
                      PluginConfiguration configuration,
                      ShopService shopService,
                      DirtEconomyService economyService) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.shopService = shopService;
        this.economyService = economyService;
        reloadFromConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        configuration.reload(plugin.getConfig());
        reloadFromConfig();
        spawnAll();
    }

    public void reloadFromConfig() {
        despawnAll();
        definitions.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("npcs");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection npcSection = section.getConfigurationSection(key);
            if (npcSection == null) {
                continue;
            }
            Location location = readLocation(npcSection);
            if (location == null) {
                plugin.getLogger().warning("NPC " + key + " 缺少位置或世界，已跳过。");
                continue;
            }
            String name = npcSection.getString("name", ChatColor.YELLOW + key);
            String typeName = npcSection.getString("type", "VILLAGER");
            EntityType entityType = EntityType.VILLAGER;
            try {
                entityType = EntityType.valueOf(typeName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("NPC " + key + " 的生物类型 " + typeName + " 无效，已使用 VILLAGER。");
            }
            String behaviorKey = npcSection.getString("behavior", "SHOP");
            NpcBehavior behavior = NpcBehavior.SHOP;
            try {
                behavior = NpcBehavior.valueOf(behaviorKey.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("NPC " + key + " 的行为 " + behaviorKey + " 无效，已使用 SHOP。");
            }
            List<String> dialogue = npcSection.getStringList("dialogue");
            NpcDefinition definition = new NpcDefinition(key, name, entityType, location, behavior, dialogue);
            definitions.put(key, definition);
        }
    }

    private Location readLocation(ConfigurationSection npcSection) {
        String worldName = npcSection.getString("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "无法找到世界 {0}，NPC 将不会生成。", worldName);
            return null;
        }
        double x = npcSection.getDouble("x");
        double y = npcSection.getDouble("y");
        double z = npcSection.getDouble("z");
        float yaw = (float) npcSection.getDouble("yaw");
        float pitch = (float) npcSection.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void spawnAll() {
        despawnAll();
        for (NpcDefinition definition : definitions.values()) {
            spawn(definition);
        }
    }

    public void despawnAll() {
        for (UUID uuid : new ArrayList<>(activeNpcIds.keySet())) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        activeNpcIds.clear();
    }

    public Optional<NpcDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Optional<NpcDefinition> getDefinition(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(NPC_ID_PREFIX)) {
                String id = tag.substring(NPC_ID_PREFIX.length());
                return getDefinition(id);
            }
        }
        return Optional.empty();
    }

    public boolean isNpc(Entity entity) {
        return getDefinition(entity).isPresent();
    }

    public Collection<NpcDefinition> list() {
        return List.copyOf(definitions.values());
    }

    public boolean create(String id, Location location, NpcBehavior behavior, EntityType type, String displayName) {
        if (id == null || location == null) {
            return false;
        }
        String normalizedId = id.toLowerCase(Locale.ROOT);
        String name = displayName != null ? displayName : ChatColor.YELLOW + normalizedId;
        NpcDefinition definition = new NpcDefinition(normalizedId, name, type, location.clone(), behavior, List.of());
        definitions.put(normalizedId, definition);
        saveDefinition(definition);
        respawn(normalizedId);
        return true;
    }

    public boolean remove(String id) {
        if (!definitions.containsKey(id)) {
            return false;
        }
        definitions.remove(id);
        plugin.getConfig().set("npcs." + id, null);
        plugin.saveConfig();
        despawn(id);
        return true;
    }

    public boolean move(String id, Location location) {
        NpcDefinition definition = definitions.get(id);
        if (definition == null) {
            return false;
        }
        NpcDefinition updated = new NpcDefinition(
                definition.id(),
                definition.displayName(),
                definition.entityType(),
                location.clone(),
                definition.behavior(),
                definition.dialogue()
        );
        definitions.put(id, updated);
        saveDefinition(updated);
        respawn(id);
        return true;
    }

    public boolean rename(String id, String displayName) {
        NpcDefinition definition = definitions.get(id);
        if (definition == null) {
            return false;
        }
        NpcDefinition updated = new NpcDefinition(
                definition.id(),
                displayName,
                definition.entityType(),
                definition.location(),
                definition.behavior(),
                definition.dialogue()
        );
        definitions.put(id, updated);
        saveDefinition(updated);
        respawn(id);
        return true;
    }

    public boolean changeType(String id, EntityType type) {
        NpcDefinition definition = definitions.get(id);
        if (definition == null) {
            return false;
        }
        NpcDefinition updated = new NpcDefinition(
                definition.id(),
                definition.displayName(),
                type,
                definition.location(),
                definition.behavior(),
                definition.dialogue()
        );
        definitions.put(id, updated);
        saveDefinition(updated);
        respawn(id);
        return true;
    }

    public boolean changeBehavior(String id, NpcBehavior behavior) {
        NpcDefinition definition = definitions.get(id);
        if (definition == null) {
            return false;
        }
        NpcDefinition updated = new NpcDefinition(
                definition.id(),
                definition.displayName(),
                definition.entityType(),
                definition.location(),
                behavior,
                definition.dialogue()
        );
        definitions.put(id, updated);
        saveDefinition(updated);
        respawn(id);
        return true;
    }

    public boolean changeDialogue(String id, List<String> lines) {
        NpcDefinition definition = definitions.get(id);
        if (definition == null) {
            return false;
        }
        NpcDefinition updated = new NpcDefinition(
                definition.id(),
                definition.displayName(),
                definition.entityType(),
                definition.location(),
                definition.behavior(),
                new ArrayList<>(lines)
        );
        definitions.put(id, updated);
        saveDefinition(updated);
        respawn(id);
        return true;
    }

    public void handleDefaultAction(Player player, NpcDefinition definition) {
        if (player == null || definition == null) {
            return;
        }
        for (String line : definition.dialogue()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
        switch (definition.behavior()) {
            case SHOP -> shopService.openShop(player);
            case MARKET -> player.performCommand("dirtshop market browse");
            case BALANCE -> player.sendMessage(ChatColor.AQUA + "当前泥土币: " + economyService.getBalance(player));
            case SCRIPT_ONLY -> {
                // Handled by scripts.
            }
        }
    }

    private void spawn(NpcDefinition definition) {
        Location location = definition.location();
        if (location == null || location.getWorld() == null) {
            return;
        }
        Entity entity = location.getWorld().spawnEntity(location, definition.entityType());
        entity.addScoreboardTag(NPC_TAG);
        entity.addScoreboardTag(NPC_ID_PREFIX + definition.id());
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', definition.displayName()));
        entity.setCustomNameVisible(true);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setCollidable(false);
            living.setInvulnerable(true);
            living.setRemoveWhenFarAway(false);
            living.setCanPickupItems(false);
            living.setSilent(true);
            if (living.getEquipment() != null) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    living.getEquipment().setItem(slot, null);
                }
            }
        }
        activeNpcIds.put(entity.getUniqueId(), definition.id());
    }

    private void despawn(String id) {
        activeNpcIds.entrySet().removeIf(entry -> {
            if (!entry.getValue().equals(id)) {
                return false;
            }
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity != null) {
                entity.remove();
            }
            return true;
        });
    }

    private void respawn(String id) {
        despawn(id);
        NpcDefinition definition = definitions.get(id);
        if (definition != null) {
            spawn(definition);
        }
    }

    private void saveDefinition(NpcDefinition definition) {
        String path = "npcs." + definition.id();
        Location location = definition.location();
        plugin.getConfig().set(path + ".name", definition.displayName());
        plugin.getConfig().set(path + ".type", definition.entityType().name());
        plugin.getConfig().set(path + ".world", location.getWorld() != null ? location.getWorld().getName() : null);
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".yaw", location.getYaw());
        plugin.getConfig().set(path + ".pitch", location.getPitch());
        plugin.getConfig().set(path + ".behavior", definition.behavior().name());
        plugin.getConfig().set(path + ".dialogue", definition.dialogue());
        plugin.saveConfig();
    }
}
