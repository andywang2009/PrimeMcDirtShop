package com.primemcdirtshop.dirtshop.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public class DirtShopHolder implements InventoryHolder {

    private final Map<Integer, ShopTrade> trades;

    public DirtShopHolder(Map<Integer, ShopTrade> trades) {
        this.trades = trades;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public Map<Integer, ShopTrade> getTrades() {
        return trades;
    }
}
