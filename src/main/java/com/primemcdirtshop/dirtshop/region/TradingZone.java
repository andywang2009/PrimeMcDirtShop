package com.primemcdirtshop.dirtshop.region;

import org.bukkit.Location;
import org.bukkit.World;

public class TradingZone {

    private final String id;
    private final World world;
    private final Location center;
    private final double radius;
    private final String shopkeeper;
    private double commission;
    private long earnings;

    public TradingZone(String id, World world, Location center, double radius, String shopkeeper, double commission, long earnings) {
        this.id = id;
        this.world = world;
        this.center = center;
        this.radius = radius;
        this.shopkeeper = shopkeeper;
        this.commission = commission;
        this.earnings = earnings;
    }

    public String getId() {
        return id;
    }

    public Location getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public String getShopkeeper() {
        return shopkeeper;
    }

    public double getCommission() {
        return commission;
    }

    public void setCommission(double commission) {
        this.commission = commission;
    }

    public long getEarnings() {
        return earnings;
    }

    public void addEarnings(long delta) {
        earnings += Math.max(0, delta);
    }

    public boolean contains(Location location) {
        if (!location.getWorld().equals(world)) {
            return false;
        }
        return center.distanceSquared(location) <= radius * radius;
    }
}
