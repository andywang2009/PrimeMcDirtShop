package com.primemcdirtshop.dirtshop.scripting;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.economy.DirtEconomyService;
import com.primemcdirtshop.dirtshop.tools.ToolModificationService;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class ScriptService {

    private final JavaPlugin plugin;
    private final PluginConfiguration configuration;
    private final DirtEconomyService economyService;
    private final ToolModificationService toolModificationService;

    private final CopyOnWriteArrayList<DirtScriptLibrary.BlockRewardModifier> blockRewardModifiers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DirtScriptLibrary.WelcomeHook> welcomeHooks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DirtScriptLibrary.ToolDecorator> toolDecorators = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DirtScriptLibrary.NpcInteractionHandler> npcHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> loadedScripts = new CopyOnWriteArrayList<>();

    public ScriptService(JavaPlugin plugin,
                         PluginConfiguration configuration,
                         DirtEconomyService economyService,
                         ToolModificationService toolModificationService) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.economyService = economyService;
        this.toolModificationService = toolModificationService;
    }

    public void loadScripts() {
        blockRewardModifiers.clear();
        welcomeHooks.clear();
        toolDecorators.clear();
        npcHandlers.clear();
        loadedScripts.clear();

        File scriptsDirectory = new File(plugin.getDataFolder(), "scripts");
        if (!scriptsDirectory.exists() && !scriptsDirectory.mkdirs()) {
            plugin.getLogger().warning("无法创建 scripts 目录，跳过脚本加载。");
            return;
        }

        File[] scripts = scriptsDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".js"));
        if (scripts == null || scripts.length == 0) {
            plugin.getLogger().info("未检测到脚本文件，使用默认行为。");
            return;
        }

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine probe = manager.getEngineByName("nashorn");
        if (probe == null) {
            plugin.getLogger().severe("未找到 Nashorn JavaScript 引擎。请检查依赖。");
            return;
        }
        for (File script : scripts) {
            ScriptEngine engine = manager.getEngineByName("nashorn");
            DirtScriptLibrary library = new DirtScriptLibrary(
                    plugin,
                    economyService,
                    toolModificationService,
                    blockRewardModifiers,
                    welcomeHooks,
                    toolDecorators,
                    npcHandlers
            );
            Bindings bindings = engine.createBindings();
            bindings.put("library", library);
            bindings.put("plugin", plugin);
            bindings.put("economy", economyService);
            bindings.put("configuration", configuration);
            engine.setBindings(bindings, javax.script.ScriptContext.ENGINE_SCOPE);
            try (Reader reader = new InputStreamReader(new FileInputStream(script), StandardCharsets.UTF_8)) {
                engine.eval(reader);
                loadedScripts.add(script.getName());
                plugin.getLogger().info("已加载脚本: " + script.getName());
            } catch (IOException | ScriptException ex) {
                plugin.getLogger().log(Level.SEVERE, "加载脚本 " + script.getName() + " 时出错", ex);
            }
        }
    }

    public List<String> getLoadedScripts() {
        return List.copyOf(loadedScripts);
    }

    public void reloadScripts() {
        loadScripts();
    }

    public long modifyBlockReward(Player player, Material material, long baseReward) {
        long reward = baseReward;
        for (DirtScriptLibrary.BlockRewardModifier modifier : blockRewardModifiers) {
            try {
                reward = modifier.modify(player, material, reward);
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "脚本在修改挖掘奖励时抛出异常", throwable);
            }
        }
        return reward;
    }

    public void fireWelcome(Player player) {
        for (DirtScriptLibrary.WelcomeHook hook : welcomeHooks) {
            try {
                hook.handle(player);
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "脚本在欢迎玩家时抛出异常", throwable);
            }
        }
    }

    public void decorateTool(Player player, ItemStack stack) {
        if (stack == null) {
            return;
        }
        for (DirtScriptLibrary.ToolDecorator decorator : toolDecorators) {
            try {
                decorator.decorate(player, stack);
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "脚本在装饰工具时抛出异常", throwable);
            }
        }
    }

    public void decorateInventory(Player player) {
        if (player == null) {
            return;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            decorateTool(player, stack);
        }
    }

    public boolean handleNpcInteraction(Player player, String npcId, Entity entity) {
        boolean handled = false;
        for (DirtScriptLibrary.NpcInteractionHandler handler : npcHandlers) {
            try {
                if (handler.handle(player, npcId, entity)) {
                    handled = true;
                }
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "脚本在处理 NPC 交互时抛出异常", throwable);
            }
        }
        return handled;
    }

    public void shutdown() {
        blockRewardModifiers.clear();
        welcomeHooks.clear();
        toolDecorators.clear();
        npcHandlers.clear();
        loadedScripts.clear();
    }
}
