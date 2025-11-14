package com.primemcdirtshop.dirtshop.market;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;

public record MarketListing(UUID id, UUID seller, ItemStack item, int amount, long price, Instant createdAt) {
}
