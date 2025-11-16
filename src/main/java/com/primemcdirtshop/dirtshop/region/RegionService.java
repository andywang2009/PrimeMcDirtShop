package com.primemcdirtshop.dirtshop.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class RegionService {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private Location center;
    private double radius;
    private boolean requireInside;
    private String worldName;
    private double mainEarnings;
    private String mainShopkeeper;
    private final Map<String, TradingZone> zones = new HashMap<>();
    private RegionGeneratorSettings generatorSettings;
    private final Random random = new Random();

    public RegionService(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        loadRegion();
        loadGeneratorSettings();
        loadZones();
    }

    private void loadRegion() {
        ConfigurationSection section = config.getConfigurationSection("region");
        if (section == null) {
            return;
        }
        this.requireInside = section.getBoolean("require-inside", false);
        this.worldName = section.getString("world");
        this.mainShopkeeper = section.getString("shopkeeper", "&6泥土管家");
        this.mainEarnings = section.getDouble("main-earnings", 0);
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = section.getDouble("center.x");
                double y = section.getDouble("center.y");
                double z = section.getDouble("center.z");
                this.radius = section.getDouble("radius", 0);
                this.center = new Location(world, x, y, z);
            }
        }
    }

    private void loadGeneratorSettings() {
        ConfigurationSection section = config.getConfigurationSection("region.generator");
        if (section == null) {
            generatorSettings = new RegionGeneratorSettings();
            return;
        }
        generatorSettings = new RegionGeneratorSettings();
        generatorSettings.world = section.getString("world", worldName);
        generatorSettings.maxDistance = section.getDouble("max-distance", 1000);
        generatorSettings.minDistance = section.getDouble("min-distance", 100);
        generatorSettings.minRadius = section.getDouble("min-radius", 12);
        generatorSettings.maxRadius = section.getDouble("max-radius", 25);
        generatorSettings.attempts = section.getInt("attempts", 16);
        generatorSettings.yMin = section.getInt("safe-y-min", 60);
        generatorSettings.yMax = section.getInt("safe-y-max", 120);
        generatorSettings.shopkeeperPool = section.getStringList("shopkeepers");
        generatorSettings.originX = section.getDouble("origin.x", 0);
        generatorSettings.originZ = section.getDouble("origin.z", 0);
    }

    private void loadZones() {
        zones.clear();
        ConfigurationSection section = config.getConfigurationSection("region.dynamic-zones");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(id);
            if (node == null) {
                continue;
            }
            String worldName = node.getString("world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null) {
                continue;
            }
            double x = node.getDouble("x");
            double y = node.getDouble("y");
            double z = node.getDouble("z");
            double radius = node.getDouble("radius", 12);
            String shopkeeper = node.getString("shopkeeper", "流动商人");
            double earnings = node.getDouble("earnings", 0);
            Location location = new Location(world, x, y, z);
            zones.put(id, new TradingZone(id, location, radius, shopkeeper, earnings, false));
        }
    }

    public boolean isRegionRequired() {
        return requireInside;
    }

    public boolean isInsideRegion(Location location) {
        ensureCenter();
        if (center == null || radius <= 0) {
            return !isRegionRequired();
        }
        if (!center.getWorld().equals(location.getWorld())) {
            return false;
        }
        return center.distanceSquared(location) <= radius * radius;
    }

    public boolean canTrade(Player player) {
        if (hasVipBypass(player)) {
            return true;
        }
        if (!isRegionRequired()) {
            return true;
        }
        return isInsideRegion(player.getLocation()) || isInsideDynamicZone(player.getLocation());
    }

    private boolean hasVipBypass(Player player) {
        return player.hasPermission("primemc.perm.pro") || player.hasPermission("primemc.perm.ultra");
    }

    public boolean isInsideDynamicZone(Location location) {
        return zones.values().stream().anyMatch(zone -> zone.isInside(location));
    }

    public Optional<Location> getCenter() {
        ensureCenter();
        return Optional.ofNullable(center);
    }

    public double getRadius() {
        return radius;
    }

    public void setRegion(Player player, double radius) {
        this.center = player.getLocation();
        this.radius = radius;
        this.worldName = center.getWorld().getName();

        config.set("region.world", worldName);
        config.set("region.center.x", center.getX());
        config.set("region.center.y", center.getY());
        config.set("region.center.z", center.getZ());
        config.set("region.radius", radius);
        config.set("region.require-inside", requireInside);
        plugin.saveConfig();
    }

    public void setRequireInside(boolean requireInside) {
        this.requireInside = requireInside;
        config.set("region.require-inside", requireInside);
        plugin.saveConfig();
    }

    public Optional<TradingZone> resolveZone(Location location) {
        if (isInsideRegion(location)) {
            ensureCenter();
            if (center == null) {
                return Optional.empty();
            }
            return Optional.of(new TradingZone("main", center, radius, mainShopkeeper, mainEarnings, true));
        }
        return zones.values().stream().filter(zone -> zone.isInside(location)).findFirst();
    }

    public Collection<TradingZone> listZones() {
        return new ArrayList<>(zones.values());
    }

    public Optional<TradingZone> createRandomZone() {
        if (generatorSettings == null || generatorSettings.world == null) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(generatorSettings.world);
        if (world == null) {
            return Optional.empty();
        }
        for (int i = 0; i < generatorSettings.attempts; i++) {
            double distance = generatorSettings.minDistance + random.nextDouble() * (generatorSettings.maxDistance - generatorSettings.minDistance);
            double angle = random.nextDouble() * Math.PI * 2;
            double x = generatorSettings.originX + Math.cos(angle) * distance;
            double z = generatorSettings.originZ + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z);
            if (y < generatorSettings.yMin || y > generatorSettings.yMax) {
                continue;
            }
            Location location = new Location(world, x + 0.5, y + 1, z + 0.5);
            double zoneRadius = generatorSettings.minRadius + random.nextDouble() * (generatorSettings.maxRadius - generatorSettings.minRadius);
            String id = "zone-" + System.currentTimeMillis();
            String shopkeeper = generatorSettings.shopkeeperPool.isEmpty()
                    ? "巡回商人"
                    : generatorSettings.shopkeeperPool.get(random.nextInt(generatorSettings.shopkeeperPool.size()));
            TradingZone zone = new TradingZone(id, location, zoneRadius, shopkeeper, 0, false);
            zones.put(id, zone);
            saveZone(zone);
            return Optional.of(zone);
        }
        return Optional.empty();
    }

    public void recordZoneRevenue(Location location, double amount) {
        if (amount <= 0) {
            return;
        }
        resolveZone(location).ifPresent(zone -> {
            zone.addEarnings(amount);
            if (zone.isPrimary()) {
                mainEarnings += amount;
                config.set("region.main-earnings", mainEarnings);
            } else {
                config.set("region.dynamic-zones." + zone.getId() + ".earnings", zone.getEarnings());
            }
            plugin.saveConfig();
        });
    }

    public String getShopkeeperName(Location location) {
        return resolveZone(location).map(TradingZone::getShopkeeper).orElse(mainShopkeeper);
    }

    private void saveZone(TradingZone zone) {
        String path = "region.dynamic-zones." + zone.getId();
        config.set(path + ".world", zone.getCenter().getWorld().getName());
        config.set(path + ".x", zone.getCenter().getX());
        config.set(path + ".y", zone.getCenter().getY());
        config.set(path + ".z", zone.getCenter().getZ());
        config.set(path + ".radius", zone.getRadius());
        config.set(path + ".shopkeeper", zone.getShopkeeper());
        config.set(path + ".earnings", zone.getEarnings());
        plugin.saveConfig();
    }

    private void ensureCenter() {
        if (center == null && worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = config.getDouble("region.center.x");
                double y = config.getDouble("region.center.y");
                double z = config.getDouble("region.center.z");
                this.center = new Location(world, x, y, z);
                this.radius = config.getDouble("region.radius", radius);
            }
        }
    }

    private static class RegionGeneratorSettings {
        private String world;
        private double maxDistance;
        private double minDistance;
        private double minRadius;
        private double maxRadius;
        private int attempts;
        private int yMin;
        private int yMax;
        private List<String> shopkeeperPool = new ArrayList<>();
        private double originX;
        private double originZ;
    }
}
