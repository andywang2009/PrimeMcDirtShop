package com.primemcdirtshop.dirtshop.npc;

import com.primemcdirtshop.dirtshop.scripting.ScriptService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Optional;

public class NpcListener implements Listener {

    private final NpcService npcService;
    private final ScriptService scriptService;

    public NpcListener(NpcService npcService, ScriptService scriptService) {
        this.npcService = npcService;
        this.scriptService = scriptService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        event.setCancelled(true);
        handleInteraction(event.getPlayer(), event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractAtEntityEvent event) {
        event.setCancelled(true);
        handleInteraction(event.getPlayer(), event.getRightClicked());
    }

    private void handleInteraction(Player player, Entity entity) {
        Optional<NpcDefinition> definitionOptional = npcService.getDefinition(entity);
        if (definitionOptional.isEmpty()) {
            return;
        }
        NpcDefinition definition = definitionOptional.get();
        player.closeInventory();
        boolean handled = scriptService.handleNpcInteraction(player, definition.id(), entity);
        if (!handled) {
            npcService.handleDefaultAction(player, definition);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (npcService.isNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (npcService.isNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
