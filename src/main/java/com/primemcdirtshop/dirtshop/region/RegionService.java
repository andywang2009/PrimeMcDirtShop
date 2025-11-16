package com.primemcdirtshop.dirtshop.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class RegionService {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private Location center;
    private double radius;
    private boolean requireInside;
    private String worldName;
    private final List<TradingZone> tradingZones = new ArrayList<>();
    private double scatterRadius;
    private double minZoneRadius;
    private double maxZoneRadius;
    private int zoneCount;
    private double minCommission;
    private double maxCommission;
    private String tradeWorldName;
    private List<String> shopkeepers = new ArrayList<>();
    private final Random random = new Random();

    public RegionService(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        loadRegion();
        loadTradingZones();
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

    private void loadTradingZones() {
        ConfigurationSection section = config.getConfigurationSection("trade-zones");
        if (section == null) {
            return;
        }
        this.tradeWorldName = section.getString("world", worldName);
        this.scatterRadius = section.getDouble("scatter-radius", 256d);
        ConfigurationSection radiusSection = section.getConfigurationSection("radius");
        this.minZoneRadius = radiusSection != null ? radiusSection.getDouble("min", 16d) : 16d;
        this.maxZoneRadius = radiusSection != null ? radiusSection.getDouble("max", 32d) : 32d;
        this.zoneCount = section.getInt("count", 3);
        this.minCommission = section.getDouble("shopkeeper-commission.min", 0.20d);
        this.maxCommission = section.getDouble("shopkeeper-commission.max", 0.25d);
        this.shopkeepers = new ArrayList<>(section.getStringList("shopkeepers"));
        if (shopkeepers.isEmpty()) {
            shopkeepers.add("泥土守卫");
            shopkeepers.add("泥土大叔");
        }
        World world = tradeWorldName != null ? Bukkit.getWorld(tradeWorldName) : null;
        if (world == null) {
            return;
        }
        ConfigurationSection zonesSection = section.getConfigurationSection("zones");
        tradingZones.clear();
        if (zonesSection != null) {
            for (String key : zonesSection.getKeys(false)) {
                ConfigurationSection zoneSection = zonesSection.getConfigurationSection(key);
                if (zoneSection == null) {
                    continue;
                }
                double x = zoneSection.getDouble("center.x");
                double y = zoneSection.getDouble("center.y");
                double z = zoneSection.getDouble("center.z");
                double zoneRadius = zoneSection.getDouble("radius", minZoneRadius);
                String shopkeeper = zoneSection.getString("shopkeeper", randomShopkeeper());
                double commission = zoneSection.getDouble("commission", minCommission);
                long earnings = zoneSection.getLong("earnings", 0L);
                TradingZone zone = new TradingZone(key, world, new Location(world, x, y, z), zoneRadius, shopkeeper, commission, earnings);
                tradingZones.add(zone);
            }
        }
        ensureTradeZoneCount(world);
        saveTradeZones();
    }

    private void ensureTradeZoneCount(World world) {
        if (world == null) {
            return;
        }
        while (tradingZones.size() < Math.max(1, zoneCount)) {
            String id = "zone-" + UUID.randomUUID();
            tradingZones.add(createRandomZone(world, id));
        }
    }

    private TradingZone createRandomZone(World world, String id) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = random.nextDouble() * scatterRadius;
        double x = distance * Math.cos(angle);
        double z = distance * Math.sin(angle);
        double y = world.getHighestBlockYAt((int) Math.round(x), (int) Math.round(z)) + 1;
        Location zoneCenter = new Location(world, x, y, z);
        double zoneRadius = minZoneRadius + (random.nextDouble() * Math.max(1d, maxZoneRadius - minZoneRadius));
        double commission = minCommission + (random.nextDouble() * Math.max(0.01d, maxCommission - minCommission));
        return new TradingZone(id, world, zoneCenter, zoneRadius, randomShopkeeper(), commission, 0L);
    }

    private void saveTradeZones() {
        ConfigurationSection section = config.getConfigurationSection("trade-zones");
        if (section == null) {
            section = config.createSection("trade-zones");
        }
        ConfigurationSection zonesSection = section.createSection("zones");
        for (TradingZone zone : tradingZones) {
            ConfigurationSection zoneSection = zonesSection.createSection(zone.getId());
            zoneSection.set("center.x", zone.getCenter().getX());
            zoneSection.set("center.y", zone.getCenter().getY());
            zoneSection.set("center.z", zone.getCenter().getZ());
            zoneSection.set("radius", zone.getRadius());
            zoneSection.set("shopkeeper", zone.getShopkeeper());
            zoneSection.set("commission", zone.getCommission());
            zoneSection.set("earnings", zone.getEarnings());
        }
        plugin.saveConfig();
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

    public List<TradingZone> getTradingZones() {
        return Collections.unmodifiableList(tradingZones);
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

    public boolean canTrade(Player player) {
        if (player.hasPermission("primemc.perm.pro") || player.hasPermission("primemc.perm.ultra")) {
            return true;
        }
        return isInsideRegion(player.getLocation()) || findTradingZone(player.getLocation()).isPresent();
    }

    public Optional<TradingZone> findTradingZone(Location location) {
        ensureCenter();
        return tradingZones.stream().filter(zone -> zone.contains(location)).findFirst();
    }

    public void regenerateTradingZones() {
        World world = tradeWorldName != null ? Bukkit.getWorld(tradeWorldName) : null;
        tradingZones.clear();
        ensureTradeZoneCount(world);
        saveTradeZones();
    }

    public void updateCommissions(double rate) {
        for (TradingZone zone : tradingZones) {
            zone.setCommission(rate);
        }
        saveTradeZones();
    }

    public long recordTrade(Location location, long totalPrice) {
        Optional<TradingZone> zone = findTradingZone(location);
        if (zone.isEmpty()) {
            return 0;
        }
        long commission = Math.round(totalPrice * zone.get().getCommission());
        zone.get().addEarnings(commission);
        saveTradeZones();
        return commission;
    }

    private String randomShopkeeper() {
        return shopkeepers.get(random.nextInt(shopkeepers.size()));
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
