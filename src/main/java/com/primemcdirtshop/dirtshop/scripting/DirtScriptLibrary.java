package com.primemcdirtshop.dirtshop.scripting;

import com.primemcdirtshop.dirtshop.economy.DirtEconomyService;
import com.primemcdirtshop.dirtshop.tools.ToolModificationService;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class DirtScriptLibrary {

    private final JavaPlugin plugin;
    private final DirtEconomyService economyService;
    private final ToolModificationService toolModificationService;
    private final List<BlockRewardModifier> blockRewardModifiers;
    private final List<WelcomeHook> welcomeHooks;
    private final List<ToolDecorator> toolDecorators;
    private final List<NpcInteractionHandler> npcHandlers;

    public DirtScriptLibrary(JavaPlugin plugin,
                             DirtEconomyService economyService,
                             ToolModificationService toolModificationService,
                             CopyOnWriteArrayList<BlockRewardModifier> blockRewardModifiers,
                             CopyOnWriteArrayList<WelcomeHook> welcomeHooks,
                             CopyOnWriteArrayList<ToolDecorator> toolDecorators,
                             CopyOnWriteArrayList<NpcInteractionHandler> npcHandlers) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.toolModificationService = toolModificationService;
        this.blockRewardModifiers = blockRewardModifiers;
        this.welcomeHooks = welcomeHooks;
        this.toolDecorators = toolDecorators;
        this.npcHandlers = npcHandlers;
    }

    public void registerBlockRewardModifier(BlockRewardModifier modifier) {
        if (modifier != null) {
            blockRewardModifiers.add(modifier);
        }
    }

    public void registerWelcomeHook(WelcomeHook hook) {
        if (hook != null) {
            welcomeHooks.add(hook);
        }
    }

    public void registerToolDecorator(ToolDecorator decorator) {
        if (decorator != null) {
            toolDecorators.add(decorator);
        }
    }

    public void registerNpcInteractionHandler(NpcInteractionHandler handler) {
        if (handler != null) {
            npcHandlers.add(handler);
        }
    }

    public void onEnable(Consumer<JavaPlugin> runnable) {
        if (runnable != null) {
            runnable.accept(plugin);
        }
    }

    public DirtEconomyService economy() {
        return economyService;
    }

    public ToolModificationService tools() {
        return toolModificationService;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    @FunctionalInterface
    public interface BlockRewardModifier {
        long modify(Player player, Material material, long currentReward);
    }

    @FunctionalInterface
    public interface WelcomeHook {
        void handle(Player player);
    }

    @FunctionalInterface
    public interface ToolDecorator {
        void decorate(Player player, ItemStack itemStack);
    }

    @FunctionalInterface
    public interface NpcInteractionHandler {
        boolean handle(Player player, String npcId, Entity entity);
    }
}
