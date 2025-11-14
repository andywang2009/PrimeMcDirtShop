package com.primemcdirtshop.dirtshop.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class RegionService {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private Location center;
    private double radius;
    private boolean requireInside;
    private String worldName;

    public RegionService(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        loadRegion();
    }

    private void loadRegion() {
        ConfigurationSection section = config.getConfigurationSection("region");
        if (section == null) {
            return;
        }
        this.requireInside = section.getBoolean("require-inside", false);
        this.worldName = section.getString("world");
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
}
