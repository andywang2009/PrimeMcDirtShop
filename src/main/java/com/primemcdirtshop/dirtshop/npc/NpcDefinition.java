package com.primemcdirtshop.dirtshop.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.List;

public record NpcDefinition(String id,
                            String displayName,
                            EntityType entityType,
                            Location location,
                            NpcBehavior behavior,
                            List<String> dialogue) {

    public NpcDefinition {
        if (dialogue == null) {
            dialogue = List.of();
        }
    }
}
