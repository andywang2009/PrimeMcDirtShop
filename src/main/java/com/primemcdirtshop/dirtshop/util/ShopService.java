package com.primemcdirtshop.dirtshop.util;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.economy.DirtEconomyService;
import com.primemcdirtshop.dirtshop.economy.EconomyHealthService;
import com.primemcdirtshop.dirtshop.region.RegionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShopService {

    private final PluginConfiguration configuration;
    private final DirtEconomyService economyService;
    private final RegionService regionService;
    private final EconomyHealthService economyHealthService;

    public ShopService(PluginConfiguration configuration,
                       DirtEconomyService economyService,
                       RegionService regionService,
                       EconomyHealthService economyHealthService) {
        this.configuration = configuration;
        this.economyService = economyService;
        this.regionService = regionService;
        this.economyHealthService = economyHealthService;
    }

    public void openShop(Player player) {
        if (!regionService.canTrade(player)) {
            player.sendMessage(ChatColor.RED + "你必须在泥土商店范围内才能交易。");
            return;
        }
        List<ShopTrade> trades = configuration.getShopTrades();
        if (trades.isEmpty()) {
            player.sendMessage(ChatColor.RED + "管理员还没有配置商店商品。");
            return;
        }
        int rows = Math.max(1, (int) Math.ceil(trades.size() / 9.0));
        int size = rows * 9;
        Map<Integer, ShopTrade> mapping = new HashMap<>();
        DirtShopHolder holder = new DirtShopHolder(mapping);
        Inventory inventory = Bukkit.createInventory(holder, size, ChatColor.DARK_GREEN + "泥土商店");
        double multiplier = economyHealthService.getInflationMultiplier();
        for (int i = 0; i < trades.size(); i++) {
            ShopTrade trade = trades.get(i);
            ItemStack stack = createDisplayItem(trade, multiplier);
            inventory.setItem(i, stack);
            mapping.put(i, trade);
        }
        player.openInventory(inventory);
    }

    private ItemStack createDisplayItem(ShopTrade trade, double multiplier) {
        Material material = trade.material();
        ItemStack stack = new ItemStack(material);
        stack.setAmount(Math.min(trade.amount(), material.getMaxStackSize()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + material.name().toLowerCase().replace('_', ' '));
            long adjustedCost = Math.max(1L, Math.round(trade.cost() * multiplier));
            meta.setLore(List.of(
                    ChatColor.GRAY + "数量: " + trade.amount(),
                    ChatColor.GOLD + "价格: " + adjustedCost + " 泥土币",
                    ChatColor.GREEN + "点击购买"
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public Optional<ShopTrade> resolveTrade(Inventory inventory, int slot) {
        if (slot < 0) {
            return Optional.empty();
        }
        if (!(inventory.getHolder() instanceof DirtShopHolder holder)) {
            return Optional.empty();
        }
        if (slot >= inventory.getSize()) {
            return Optional.empty();
        }
        return Optional.ofNullable(holder.getTrades().get(slot));
    }

    public boolean buy(Player player, ShopTrade trade) {
        if (!regionService.canTrade(player)) {
            player.sendMessage(ChatColor.RED + "你必须在泥土商店范围内才能交易。");
            return false;
        }
        long cost = Math.max(1L, Math.round(trade.cost() * economyHealthService.getInflationMultiplier()));
        if (!economyService.withdraw(player.getUniqueId(), cost)) {
            player.sendMessage(ChatColor.RED + "泥土币不足!");
            return false;
        }
        ItemStack stack = new ItemStack(trade.material());
        stack.setAmount(trade.amount());
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(ChatColor.GREEN + "购买成功，花费 " + cost + " 泥土币。");
        return true;
    }

    public long getBalance(Player player) {
        return economyService.getBalance(player);
    }
}
