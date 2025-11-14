package com.primemcdirtshop.dirtshop.listeners;

import com.primemcdirtshop.dirtshop.util.ShopService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopMenuListener implements Listener {

    private final ShopService shopService;

    public ShopMenuListener(ShopService shopService) {
        this.shopService = shopService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        shopService.resolveTrade(event.getView().getTopInventory(), event.getRawSlot()).ifPresentOrElse(trade -> {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }
            if (shopService.buy(player, trade)) {
                player.sendMessage(ChatColor.GOLD + "剩余泥土币: " + shopService.getBalance(player));
            }
        }, () -> {
            if (event.getView().getTitle().contains("泥土商店")) {
                event.setCancelled(true);
            }
        });
    }
}
