package com.primemcdirtshop.dirtshop.listeners;

import com.primemcdirtshop.dirtshop.scripting.ScriptService;
import com.primemcdirtshop.dirtshop.tools.ToolModificationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class ToolModificationListener implements Listener {

    private final ToolModificationService toolModificationService;
    private final ScriptService scriptService;

    public ToolModificationListener(ToolModificationService toolModificationService, ScriptService scriptService) {
        this.toolModificationService = toolModificationService;
        this.scriptService = scriptService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (toolModificationService.shouldApplyOnJoin()) {
            toolModificationService.apply(player);
        }
        scriptService.decorateInventory(player);
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null) {
            toolModificationService.apply(item);
            scriptService.decorateTool(player, item);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null) {
            return;
        }
        ItemStack clone = result.clone();
        toolModificationService.apply(clone);
        Player viewer = null;
        if (!event.getViewers().isEmpty() && event.getViewers().get(0) instanceof Player player) {
            viewer = player;
        }
        scriptService.decorateTool(viewer, clone);
        event.getInventory().setResult(clone);
    }
}
