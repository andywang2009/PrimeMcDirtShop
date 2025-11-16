package com.primemcdirtshop.dirtshop.market;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.economy.DirtEconomyService;
import com.primemcdirtshop.dirtshop.economy.EconomyHealthService;
import com.primemcdirtshop.dirtshop.region.RegionService;
import com.primemcdirtshop.dirtshop.storage.MarketStorage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarketService {

    private final MarketStorage storage;
    private final DirtEconomyService economyService;
    private final RegionService regionService;
    private final EconomyHealthService economyHealthService;
    private final PluginConfiguration configuration;

    public MarketService(MarketStorage storage,
                         DirtEconomyService economyService,
                         RegionService regionService,
                         PluginConfiguration configuration,
                         EconomyHealthService economyHealthService) {
        this.storage = storage;
        this.economyService = economyService;
        this.regionService = regionService;
        this.configuration = configuration;
        this.economyHealthService = economyHealthService;
    }

    public void loadListings() {
        storage.load();
        purgeExpired();
    }

    public void saveListings() {
        storage.save();
    }

    public List<MarketListing> getListings() {
        purgeExpired();
        return storage.getAll().stream()
                .sorted(Comparator.comparing(MarketListing::createdAt))
                .collect(Collectors.toList());
    }

    public Optional<MarketListing> getListing(UUID id) {
        purgeExpired();
        return storage.get(id);
    }

    public boolean canCreateListing(Player player) {
        purgeExpired();
        long count = storage.countForSeller(player.getUniqueId());
        return count < configuration.getMaxListingsPerPlayer();
    }

    public Optional<MarketListing> createListing(Player player, long price) {
        if (!regionService.canTrade(player)) {
            return Optional.empty();
        }
        if (!canCreateListing(player)) {
            return Optional.empty();
        }
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType().isAir()) {
            return Optional.empty();
        }
        if (price <= 0) {
            return Optional.empty();
        }
        ItemStack cloned = inHand.clone();
        int amount = cloned.getAmount();
        if (amount <= 0) {
            return Optional.empty();
        }
        player.getInventory().setItemInMainHand(null);

        MarketListing listing = new MarketListing(
                UUID.randomUUID(),
                player.getUniqueId(),
                cloned,
                amount,
                price,
                Instant.now()
        );
        storage.add(listing);
        storage.save();
        return Optional.of(listing);
    }

    public boolean purchase(Player buyer, UUID id) {
        Optional<MarketListing> optional = getListing(id);
        if (optional.isEmpty()) {
            return false;
        }
        if (!regionService.canTrade(buyer)) {
            return false;
        }
        MarketListing listing = optional.get();
        if (listing.seller().equals(buyer.getUniqueId())) {
            return false;
        }
        if (!economyService.withdraw(buyer.getUniqueId(), listing.price())) {
            return false;
        }
        long commission = regionService.recordTrade(buyer.getLocation(), listing.price());
        if (commission <= 0) {
            commission = Math.round(listing.price() * economyHealthService.getShopkeeperTaxRate());
        }
        long payout = Math.max(0, listing.price() - commission);
        economyService.deposit(listing.seller(), payout);
        ItemStack item = listing.item().clone();
        buyer.getInventory().addItem(item).values()
                .forEach(remaining -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), remaining));
        storage.remove(id);
        storage.save();
        return true;
    }

    public void cancelListing(UUID id, Player requester) {
        storage.get(id).ifPresent(listing -> {
            if (!listing.seller().equals(requester.getUniqueId())) {
                return;
            }
            ItemStack item = listing.item().clone();
            requester.getInventory().addItem(item).values()
                    .forEach(remaining -> requester.getWorld().dropItemNaturally(requester.getLocation(), remaining));
            storage.remove(id);
            storage.save();
        });
    }

    public boolean canTrade(Player player) {
        return regionService.canTrade(player);
    }

    private void purgeExpired() {
        Duration lifetime = Duration.ofHours(configuration.getListingExpiryHours());
        Instant cutoff = Instant.now().minus(lifetime);
        List<UUID> expired = storage.getAll().stream()
                .filter(listing -> listing.createdAt().isBefore(cutoff))
                .map(MarketListing::id)
                .collect(Collectors.toList());
        if (!expired.isEmpty()) {
            expired.forEach(storage::remove);
            storage.save();
        }
    }
}
