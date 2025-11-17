package com.primemcdirtshop.dirtshop;

import com.primemcdirtshop.dirtshop.arsenal.WeaponService;
import com.primemcdirtshop.dirtshop.commands.DirtShopCommand;
import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.economy.DirtBreakListener;
import com.primemcdirtshop.dirtshop.economy.DirtEconomyService;
import com.primemcdirtshop.dirtshop.economy.EconomyAnalyticsService;
import com.primemcdirtshop.dirtshop.listeners.ShopMenuListener;
import com.primemcdirtshop.dirtshop.listeners.ToolModificationListener;
import com.primemcdirtshop.dirtshop.listeners.WelcomeListener;
import com.primemcdirtshop.dirtshop.market.MarketService;
import com.primemcdirtshop.dirtshop.npc.NpcListener;
import com.primemcdirtshop.dirtshop.npc.NpcService;
import com.primemcdirtshop.dirtshop.progression.PlayerStatsListener;
import com.primemcdirtshop.dirtshop.progression.PlayerStatsService;
import com.primemcdirtshop.dirtshop.progression.PlayerStatsStorage;
import com.primemcdirtshop.dirtshop.region.RegionService;
import com.primemcdirtshop.dirtshop.scripting.ScriptService;
import com.primemcdirtshop.dirtshop.storage.MarketStorage;
import com.primemcdirtshop.dirtshop.storage.PlayerDataRepository;
import com.primemcdirtshop.dirtshop.tools.ToolModificationService;
import com.primemcdirtshop.dirtshop.util.ShopService;
import com.primemcdirtshop.dirtshop.arsenal.WeaponSkillListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class PrimeMcDirtShopPlugin extends JavaPlugin {

    private AnnotationConfigApplicationContext context;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("无法创建插件数据目录，后续可能无法保存配置或脚本。");
        }
        context = new AnnotationConfigApplicationContext();
        context.registerBean(JavaPlugin.class, () -> this);
        context.registerBean(PluginConfiguration.class, () -> new PluginConfiguration(getConfig()));
        context.registerBean(PlayerDataRepository.class, () -> new PlayerDataRepository(this));
        context.registerBean(PlayerStatsStorage.class, () -> new PlayerStatsStorage(this));
        context.registerBean(PlayerStatsService.class, () -> new PlayerStatsService(context.getBean(PlayerStatsStorage.class)));
        context.registerBean(MarketStorage.class, () -> new MarketStorage(this));
        context.registerBean(ToolModificationService.class, () -> new ToolModificationService(context.getBean(PluginConfiguration.class)));
        context.registerBean(DirtEconomyService.class, () -> new DirtEconomyService(
                context.getBean(PlayerDataRepository.class),
                context.getBean(PluginConfiguration.class),
                context.getBean(ToolModificationService.class)
        ));
        context.registerBean(EconomyAnalyticsService.class, () -> new EconomyAnalyticsService(context.getBean(PlayerDataRepository.class)));
        context.registerBean(RegionService.class, () -> new RegionService(this, getConfig()));
        context.registerBean(WeaponService.class, () -> new WeaponService(
                context.getBean(PluginConfiguration.class),
                context.getBean(PlayerStatsService.class)
        ));
        context.registerBean(MarketService.class, () -> new MarketService(
                context.getBean(MarketStorage.class),
                context.getBean(DirtEconomyService.class),
                context.getBean(RegionService.class),
                context.getBean(PluginConfiguration.class),
                context.getBean(EconomyAnalyticsService.class)
        ));
        context.registerBean(ShopService.class, () -> new ShopService(
                context.getBean(PluginConfiguration.class),
                context.getBean(DirtEconomyService.class),
                context.getBean(RegionService.class),
                context.getBean(EconomyAnalyticsService.class),
                context.getBean(PlayerStatsService.class)
        ));
        context.registerBean(ScriptService.class, () -> new ScriptService(
                this,
                context.getBean(PluginConfiguration.class),
                context.getBean(DirtEconomyService.class),
                context.getBean(ToolModificationService.class)
        ));
        context.registerBean(NpcService.class, () -> new NpcService(
                this,
                context.getBean(PluginConfiguration.class),
                context.getBean(ShopService.class),
                context.getBean(DirtEconomyService.class)
        ));
        context.registerBean(DirtShopCommand.class, () -> new DirtShopCommand(
                context.getBean(DirtEconomyService.class),
                context.getBean(MarketService.class),
                context.getBean(ShopService.class),
                context.getBean(RegionService.class),
                context.getBean(NpcService.class),
                context.getBean(ScriptService.class),
                context.getBean(ToolModificationService.class),
                context.getBean(PlayerStatsService.class),
                context.getBean(WeaponService.class)
        ));
        context.registerBean(DirtBreakListener.class, () -> new DirtBreakListener(
                context.getBean(DirtEconomyService.class),
                context.getBean(RegionService.class),
                context.getBean(ScriptService.class)
        ));
        context.registerBean(ShopMenuListener.class, () -> new ShopMenuListener(context.getBean(ShopService.class)));
        context.registerBean(ToolModificationListener.class, () -> new ToolModificationListener(
                context.getBean(ToolModificationService.class),
                context.getBean(ScriptService.class)
        ));
        context.registerBean(WelcomeListener.class, () -> new WelcomeListener(
                context.getBean(PluginConfiguration.class),
                context.getBean(ScriptService.class)
        ));
        context.registerBean(NpcListener.class, () -> new NpcListener(
                context.getBean(NpcService.class),
                context.getBean(ScriptService.class)
        ));
        context.registerBean(PlayerStatsListener.class, () -> new PlayerStatsListener(context.getBean(PlayerStatsService.class)));
        context.registerBean(WeaponSkillListener.class, () -> new WeaponSkillListener(context.getBean(WeaponService.class)));
        context.refresh();

        saveResource("scripts/sample.js", false);
        registerListeners();
        registerCommands();

        context.getBean(MarketService.class).loadListings();
        context.getBean(ScriptService.class).loadScripts();
        context.getBean(NpcService.class).spawnAll();
        Bukkit.getOnlinePlayers().forEach(player -> {
            context.getBean(ToolModificationService.class).apply(player);
            context.getBean(ScriptService.class).decorateInventory(player);
            context.getBean(PlayerStatsService.class).apply(player);
        });
        getLogger().info("PrimeMcDirtShop enabled with Spring context.");
    }

    @Override
    public void onDisable() {
        if (context != null) {
            HandlerList.unregisterAll(this);
            context.getBean(PlayerDataRepository.class).flush();
            context.getBean(MarketService.class).saveListings();
            context.getBean(NpcService.class).despawnAll();
            context.getBean(ScriptService.class).shutdown();
            context.getBean(PlayerStatsService.class).save();
            context.close();
            context = null;
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(context.getBean(DirtBreakListener.class), this);
        Bukkit.getPluginManager().registerEvents(context.getBean(ShopMenuListener.class), this);
        Bukkit.getPluginManager().registerEvents(context.getBean(ToolModificationListener.class), this);
        Bukkit.getPluginManager().registerEvents(context.getBean(WelcomeListener.class), this);
        Bukkit.getPluginManager().registerEvents(context.getBean(NpcListener.class), this);
        Bukkit.getPluginManager().registerEvents(context.getBean(PlayerStatsListener.class), this);
        Bukkit.getPluginManager().registerEvents(context.getBean(WeaponSkillListener.class), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("dirtshop");
        if (command != null) {
            DirtShopCommand executor = context.getBean(DirtShopCommand.class);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Failed to register /dirtshop command");
        }
    }
}
