package com.primemcdirtshop.dirtshop.region;

import org.bukkit.Location;

public class TradingZone {

    private final String id;
    private final Location center;
    private final double radius;
    private final String shopkeeper;
    private double earnings;
    private final boolean primary;

    public TradingZone(String id, Location center, double radius, String shopkeeper, double earnings, boolean primary) {
        this.id = id;
        this.center = center;
        this.radius = radius;
        this.shopkeeper = shopkeeper;
        this.earnings = earnings;
        this.primary = primary;
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

    public double getEarnings() {
        return earnings;
    }

    public void addEarnings(double amount) {
        earnings += amount;
    }

    public boolean isInside(Location location) {
        if (!center.getWorld().equals(location.getWorld())) {
            return false;
        }
        return center.distanceSquared(location) <= radius * radius;
    }

    public boolean isPrimary() {
        return primary;
    }
}
