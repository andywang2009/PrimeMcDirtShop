package com.primemcdirtshop.dirtshop.economy;

import com.primemcdirtshop.dirtshop.region.RegionService;
import com.primemcdirtshop.dirtshop.scripting.ScriptService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class DirtBreakListener implements Listener {

    private final DirtEconomyService economyService;
    private final RegionService regionService;
    private final ScriptService scriptService;

    public DirtBreakListener(DirtEconomyService economyService,
                             RegionService regionService,
                             ScriptService scriptService) {
        this.economyService = economyService;
        this.regionService = regionService;
        this.scriptService = scriptService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Block block = event.getBlock();
        if (regionService.isRegionRequired() && !regionService.isInsideRegion(block.getLocation())) {
            return;
        }

        Material material = block.getType();
        int fortuneLevel = player.getInventory().getItemInMainHand()
                .getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOT_BONUS_BLOCKS);
        long reward = economyService.calculateReward(material, fortuneLevel);
        reward = scriptService.modifyBlockReward(player, material, reward);
        if (reward <= 0) {
            return;
        }

        economyService.rewardForBlock(player, reward);
    }
}
