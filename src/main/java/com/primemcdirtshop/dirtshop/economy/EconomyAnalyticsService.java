package com.primemcdirtshop.dirtshop.economy;

import com.primemcdirtshop.dirtshop.storage.PlayerDataRepository;

public class EconomyAnalyticsService {

    private final PlayerDataRepository repository;
    private long recordedVolume;

    public EconomyAnalyticsService(PlayerDataRepository repository) {
        this.repository = repository;
    }

    public void recordTransaction(long amount) {
        if (amount <= 0) {
            return;
        }
        recordedVolume += amount;
    }

    public long getTotalWealth() {
        return repository.getTotalWealth();
    }

    public double resolveShopkeeperCut() {
        long wealth = getTotalWealth();
        if (recordedVolume > 200_000 || wealth > 1_000_000) {
            return 0.25;
        }
        if (recordedVolume > 100_000 || wealth > 500_000) {
            return 0.23;
        }
        return 0.20;
    }

    public long applyMarketMultiplier(long base) {
        double multiplier = determineMarketMultiplier();
        return Math.max(1, Math.round(base * multiplier));
    }

    public double determineMarketMultiplier() {
        long wealth = getTotalWealth();
        if (wealth < 100_000) {
            return 0.95;
        }
        if (wealth > 500_000) {
            return 1.10;
        }
        return 1.0;
    }
}
