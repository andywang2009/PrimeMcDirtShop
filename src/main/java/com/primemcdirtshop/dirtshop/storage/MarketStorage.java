package com.primemcdirtshop.dirtshop.storage;

import com.primemcdirtshop.dirtshop.market.MarketListing;
import com.primemcdirtshop.dirtshop.util.ItemStackSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MarketStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, MarketListing> listings = new HashMap<>();

    public MarketStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for market");
        }
        this.file = new File(plugin.getDataFolder(), "market-listings.yml");
    }

    public void load() {
        listings.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection section = yaml.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String sellerId = section.getString("seller");
                if (sellerId == null) {
                    continue;
                }
                UUID seller = UUID.fromString(sellerId);
                long price = section.getLong("price");
                int amount = section.getInt("amount");
                long created = section.getLong("created");
                String encodedItem = section.getString("item");
                if (encodedItem == null) {
                    continue;
                }
                MarketListing listing = new MarketListing(
                        id,
                        seller,
                        ItemStackSerializer.deserialize(encodedItem),
                        amount,
                        price,
                        Instant.ofEpochMilli(created)
                );
                listings.put(id, listing);
            } catch (IllegalArgumentException | IOException | ClassNotFoundException ex) {
                plugin.getLogger().warning("Failed to load listing " + key + ": " + ex.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (MarketListing listing : listings.values()) {
            ConfigurationSection section = yaml.createSection(listing.id().toString());
            section.set("seller", listing.seller().toString());
            section.set("price", listing.price());
            section.set("amount", listing.amount());
            section.set("created", listing.createdAt().toEpochMilli());
            try {
                section.set("item", ItemStackSerializer.serialize(listing.item()));
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save listing " + listing.id() + ": " + e.getMessage());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save market listings: " + e.getMessage());
        }
    }

    public Collection<MarketListing> getAll() {
        return listings.values();
    }

    public Optional<MarketListing> get(UUID id) {
        return Optional.ofNullable(listings.get(id));
    }

    public void add(MarketListing listing) {
        listings.put(listing.id(), listing);
    }

    public void remove(UUID id) {
        listings.remove(id);
    }

    public long countForSeller(UUID seller) {
        return listings.values().stream().filter(listing -> listing.seller().equals(seller)).count();
    }
}
