package com.primemcdirtshop.dirtshop.economy;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.storage.PlayerDataRepository;

public class EconomyHealthService {

    private final PlayerDataRepository repository;
    private final PluginConfiguration configuration;

    public EconomyHealthService(PlayerDataRepository repository, PluginConfiguration configuration) {
        this.repository = repository;
        this.configuration = configuration;
    }

    public long getTotalWealth() {
        return repository.getTotalBalance();
    }

    public double getInflationMultiplier() {
        double min = configuration.getRawConfig().getDouble("economy.inflation.min", 1.0d);
        double max = configuration.getRawConfig().getDouble("economy.inflation.max", 2.5d);
        long target = configuration.getRawConfig().getLong("economy.target-wealth", 50000L);
        if (target <= 0) {
            return min;
        }
        double ratio = Math.max(0d, (double) getTotalWealth() / target);
        double multiplier = Math.min(max, min + (ratio * (max - min)));
        return Math.max(min, multiplier);
    }

    public double getShopkeeperTaxRate() {
        double base = configuration.getRawConfig().getDouble("economy.shopkeeper-cut.min", 0.20d);
        double max = configuration.getRawConfig().getDouble("economy.shopkeeper-cut.max", 0.25d);
        long target = configuration.getRawConfig().getLong("economy.target-wealth", 50000L);
        if (target <= 0) {
            return base;
        }
        double ratio = Math.max(0d, Math.min(1d, (double) getTotalWealth() / (target * 1.5d)));
        double rate = base + (ratio * (max - base));
        return Math.max(base, Math.min(max, rate));
    }
}
