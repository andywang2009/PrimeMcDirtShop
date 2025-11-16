package com.primemcdirtshop.dirtshop.economy;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.storage.PlayerDataRepository;
import com.primemcdirtshop.dirtshop.tools.ToolModificationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Map;
import java.util.UUID;

public class DirtEconomyService {

    private final PlayerDataRepository repository;
    private final PluginConfiguration configuration;
    private final ToolModificationService toolModificationService;

    public DirtEconomyService(PlayerDataRepository repository,
                              PluginConfiguration configuration,
                              ToolModificationService toolModificationService) {
        this.repository = repository;
        this.configuration = configuration;
        this.toolModificationService = toolModificationService;
    }

    public long getBalance(UUID uuid) {
        return repository.getBalance(uuid);
    }

    public long getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public void deposit(UUID uuid, long amount) {
        if (amount <= 0) {
            return;
        }
        repository.addBalance(uuid, amount);
    }

    public boolean withdraw(UUID uuid, long amount) {
        if (amount <= 0) {
            return true;
        }
        long balance = repository.getBalance(uuid);
        if (balance < amount) {
            return false;
        }
        repository.setBalance(uuid, balance - amount);
        return true;
    }

    public long calculateReward(Material material, int fortuneLevel) {
        Map<Material, Integer> values = configuration.getBlockValues();
        int base = values.getOrDefault(material, 0);
        if (base <= 0) {
            return 0;
        }
        if (!configuration.isFortuneBonusEnabled() || fortuneLevel <= 0) {
            return base;
        }
        // Basic bonus: base + (fortuneLevel * base / 2)
        return base + (long) fortuneLevel * base / 2;
    }

    public void rewardForBlock(Player player, long reward) {
        if (reward <= 0) {
            return;
        }
        repository.addBalance(player.getUniqueId(), reward);
        player.sendActionBar(Component.text("+" + reward + " 泥土币", NamedTextColor.GOLD));
        damageTool(player);
    }

    private void damageTool(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().getMaxDurability() <= 0) {
            return;
        }
        int damage = toolModificationService.calculateDurabilityDamage(tool, 1);
        if (damage <= 0) {
            return;
        }
        tool.editMeta(meta -> {
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(damageable.getDamage() + damage);
            }
        });
    }
}
