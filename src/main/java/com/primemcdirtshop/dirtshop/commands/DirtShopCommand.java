package com.primemcdirtshop.dirtshop.commands;

import com.primemcdirtshop.dirtshop.armory.ArmoryService;
import com.primemcdirtshop.dirtshop.economy.DirtEconomyService;
import com.primemcdirtshop.dirtshop.economy.EconomyHealthService;
import com.primemcdirtshop.dirtshop.market.MarketListing;
import com.primemcdirtshop.dirtshop.market.MarketService;
import com.primemcdirtshop.dirtshop.npc.NpcBehavior;
import com.primemcdirtshop.dirtshop.npc.NpcDefinition;
import com.primemcdirtshop.dirtshop.npc.NpcService;
import com.primemcdirtshop.dirtshop.region.RegionService;
import com.primemcdirtshop.dirtshop.region.TradingZone;
import com.primemcdirtshop.dirtshop.scripting.ScriptService;
import com.primemcdirtshop.dirtshop.tools.ToolModificationService;
import com.primemcdirtshop.dirtshop.util.ShopService;
import com.primemcdirtshop.dirtshop.stats.PlayerStats;
import com.primemcdirtshop.dirtshop.stats.PlayerStatsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class DirtShopCommand implements CommandExecutor, TabCompleter {

    private final DirtEconomyService economyService;
    private final MarketService marketService;
    private final ShopService shopService;
    private final RegionService regionService;
    private final NpcService npcService;
    private final ScriptService scriptService;
    private final ToolModificationService toolModificationService;
    private final EconomyHealthService economyHealthService;
    private final PlayerStatsService statsService;
    private final ArmoryService armoryService;

    public DirtShopCommand(DirtEconomyService economyService,
                           MarketService marketService,
                           ShopService shopService,
                           RegionService regionService,
                           NpcService npcService,
                           ScriptService scriptService,
                           ToolModificationService toolModificationService,
                           EconomyHealthService economyHealthService,
                           PlayerStatsService statsService,
                           ArmoryService armoryService) {
        this.economyService = economyService;
        this.marketService = marketService;
        this.shopService = shopService;
        this.regionService = regionService;
        this.npcService = npcService;
        this.scriptService = scriptService;
        this.toolModificationService = toolModificationService;
        this.economyHealthService = economyHealthService;
        this.statsService = statsService;
        this.armoryService = armoryService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用泥土商店命令。");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "balance", "bal" -> handleBalance(player);
            case "shop" -> handleShop(player);
            case "market" -> handleMarket(player, args);
            case "region" -> handleRegion(player, args);
            case "install" -> handleInstall(player, args);
            case "npc" -> handleNpc(player, args);
            case "scripts" -> handleScripts(player, args);
            case "tools" -> handleTools(player);
            case "stats" -> handleStats(player);
            case "armory" -> handleArmory(player, args);
            case "zones" -> handleZones(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean handleBalance(Player player) {
        long balance = economyService.getBalance(player);
        player.sendMessage(ChatColor.GOLD + "当前泥土币: " + balance);
        return true;
    }

    private boolean handleShop(Player player) {
        shopService.openShop(player);
        return true;
    }

    private boolean handleMarket(Player player, String[] args) {
        if (args.length < 2) {
            sendMarketHelp(player);
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "browse" -> {
                if (!marketService.canTrade(player)) {
                    player.sendMessage(ChatColor.RED + "只有在交易区或拥有 primemc.perm.pro/ultra 权限时才能浏览市场。");
                    return true;
                }
                player.sendMessage(ChatColor.YELLOW + "======= 泥土市场 =======");
                List<MarketListing> listings = marketService.getListings();
                if (listings.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "当前没有上架的物品。");
                } else {
                    for (MarketListing listing : listings) {
                        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.seller());
                        player.sendMessage(ChatColor.GOLD + listing.id().toString() + ChatColor.GRAY + " - "
                                + ChatColor.GREEN + listing.item().getType() + ChatColor.GRAY + " x" + listing.amount()
                                + " / " + ChatColor.AQUA + listing.price() + " 泥土币 "
                                + ChatColor.WHITE + "卖家: " + (seller.getName() != null ? seller.getName() : "未知"));
                    }
                }
                return true;
            }
            case "list" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop market list <价格>");
                    return true;
                }
                if (!marketService.canTrade(player)) {
                    player.sendMessage(ChatColor.RED + "请前往随机交易区或升级为 VIP 后再上架物品。");
                    return true;
                }
                try {
                    long price = Long.parseLong(args[2]);
                    if (price <= 0) {
                        player.sendMessage(ChatColor.RED + "价格必须大于0。");
                        return true;
                    }
                    if (!marketService.canCreateListing(player)) {
                        player.sendMessage(ChatColor.RED + "达到上架数量上限。");
                        return true;
                    }
                    Optional<MarketListing> listing = marketService.createListing(player, price);
                    if (listing.isPresent()) {
                        player.sendMessage(ChatColor.GREEN + "物品已上架，编号: " + listing.get().id());
                    } else {
                        player.sendMessage(ChatColor.RED + "无法上架，请确认手持物品并站在商店范围内。");
                    }
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "价格必须是数字。");
                }
                return true;
            }
            case "buy" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop market buy <编号>");
                    return true;
                }
                if (!marketService.canTrade(player)) {
                    player.sendMessage(ChatColor.RED + "请前往交易区或成为 VIP 后购买。");
                    return true;
                }
                try {
                    UUID id = UUID.fromString(args[2]);
                    if (marketService.purchase(player, id)) {
                        player.sendMessage(ChatColor.GREEN + "购买成功!");
                    } else {
                        player.sendMessage(ChatColor.RED + "购买失败，可能余额不足或编号无效。");
                    }
                } catch (IllegalArgumentException ex) {
                    player.sendMessage(ChatColor.RED + "编号格式不正确。");
                }
                return true;
            }
            case "cancel" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop market cancel <编号>");
                    return true;
                }
                if (!marketService.canTrade(player)) {
                    player.sendMessage(ChatColor.RED + "请在交易区内撤销上架。");
                    return true;
                }
                try {
                    UUID id = UUID.fromString(args[2]);
                    marketService.cancelListing(id, player);
                    player.sendMessage(ChatColor.YELLOW + "尝试撤销上架，如有空位物品将返还背包。");
                } catch (IllegalArgumentException ex) {
                    player.sendMessage(ChatColor.RED + "编号格式不正确。");
                }
                return true;
            }
            default -> {
                sendMarketHelp(player);
                return true;
            }
        }
    }

    private boolean handleRegion(Player player, String[] args) {
        if (!player.hasPermission("dirtshop.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限修改商店区域。");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "用法: /dirtshop region <set|require|info> ...");
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop region set <半径>");
                    return true;
                }
                try {
                    double radius = Double.parseDouble(args[2]);
                    if (radius <= 0) {
                        player.sendMessage(ChatColor.RED + "半径必须大于0。");
                        return true;
                    }
                    regionService.setRegion(player, radius);
                    player.sendMessage(ChatColor.GREEN + "已更新泥土商店中心和范围。");
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "半径必须是数字。");
                }
                return true;
            }
            case "require" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop region require <true|false>");
                    return true;
                }
                String flag = args[2].toLowerCase(Locale.ROOT);
                if (!flag.equals("true") && !flag.equals("false")) {
                    player.sendMessage(ChatColor.RED + "只能输入 true 或 false。");
                    return true;
                }
                boolean require = Boolean.parseBoolean(flag);
                regionService.setRequireInside(require);
                player.sendMessage(ChatColor.GREEN + "已设置必须在商店范围内: " + require);
                return true;
            }
            case "info" -> {
                regionService.getCenter().ifPresentOrElse(center -> {
                    player.sendMessage(ChatColor.GOLD + "商店中心: " + center.getWorld().getName() + " ("
                            + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ() + ")");
                    player.sendMessage(ChatColor.GOLD + "半径: " + regionService.getRadius());
                    player.sendMessage(ChatColor.GOLD + "是否强制在范围内: " + regionService.isRegionRequired());
                }, () -> player.sendMessage(ChatColor.RED + "尚未设置商店范围。"));
                return true;
            }
            default -> {
                player.sendMessage(ChatColor.YELLOW + "用法: /dirtshop region <set|require|info>");
                return true;
            }
        }
    }

    private boolean handleInstall(Player player, String[] args) {
        if (!player.hasPermission("dirtshop.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限安装泥土商店。");
            return true;
        }
        double radius = 10;
        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException ignored) {
                player.sendMessage(ChatColor.YELLOW + "半径格式错误，已使用默认10格。");
            }
        }
        regionService.setRegion(player, radius);
        regionService.setRequireInside(true);
        player.sendMessage(ChatColor.GREEN + "泥土商店安装完成! 玩家需要在此范围内进行交易。");
        return true;
    }

    private boolean handleNpc(Player player, String[] args) {
        if (!player.hasPermission("dirtshop.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限管理 NPC。");
            return true;
        }
        if (args.length < 2) {
            sendNpcHelp(player);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop npc create <id> [行为] [类型] [显示名]");
                    return true;
                }
                String id = args[2].toLowerCase(Locale.ROOT);
                NpcBehavior behavior = parseBehavior(args.length >= 4 ? args[3] : "SHOP");
                EntityType type = parseEntityType(args.length >= 5 ? args[4] : "VILLAGER");
                String displayName = args.length >= 6 ? joinArgs(args, 5) : ChatColor.YELLOW + id;
                if (npcService.create(id, player.getLocation(), behavior, type, displayName)) {
                    player.sendMessage(ChatColor.GREEN + "NPC " + id + " 已创建。");
                } else {
                    player.sendMessage(ChatColor.RED + "NPC 创建失败，请检查参数。");
                }
                return true;
            }
            case "remove" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop npc remove <id>");
                    return true;
                }
                if (npcService.remove(args[2].toLowerCase(Locale.ROOT))) {
                    player.sendMessage(ChatColor.YELLOW + "NPC 已移除。");
                } else {
                    player.sendMessage(ChatColor.RED + "未找到对应 NPC。");
                }
                return true;
            }
            case "move" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop npc move <id>");
                    return true;
                }
                if (npcService.move(args[2].toLowerCase(Locale.ROOT), player.getLocation())) {
                    player.sendMessage(ChatColor.GREEN + "NPC 位置已更新。");
                } else {
                    player.sendMessage(ChatColor.RED + "未找到对应 NPC。");
                }
                return true;
            }
            case "name" -> {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop npc name <id> <显示名>");
                    return true;
                }
                String id = args[2].toLowerCase(Locale.ROOT);
                String name = joinArgs(args, 3);
                if (npcService.rename(id, name)) {
                    player.sendMessage(ChatColor.GREEN + "NPC 名称已更新。");
                } else {
                    player.sendMessage(ChatColor.RED + "未找到对应 NPC。");
                }
                return true;
            }
            case "type" -> {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop npc type <id> <类型>");
                    return true;
                }
                EntityType type = parseEntityType(args[3]);
                if (npcService.changeType(args[2].toLowerCase(Locale.ROOT), type)) {
                    player.sendMessage(ChatColor.GREEN + "NPC 类型已更新为 " + type + "。");
                } else {
                    player.sendMessage(ChatColor.RED + "未找到对应 NPC。");
                }
                return true;
            }
            case "behavior" -> {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop npc behavior <id> <行为>");
                    return true;
                }
                NpcBehavior behavior = parseBehavior(args[3]);
                if (npcService.changeBehavior(args[2].toLowerCase(Locale.ROOT), behavior)) {
                    player.sendMessage(ChatColor.GREEN + "NPC 行为已更新为 " + behavior + "。");
                } else {
                    player.sendMessage(ChatColor.RED + "未找到对应 NPC。");
                }
                return true;
            }
            case "dialogue" -> {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "用法: /dirtshop npc dialogue <id> <内容1|内容2|...>");
                    return true;
                }
                String id = args[2].toLowerCase(Locale.ROOT);
                String[] lines = joinArgs(args, 3).split("\\|");
                if (npcService.changeDialogue(id, List.of(lines))) {
                    player.sendMessage(ChatColor.GREEN + "NPC 对话已更新。");
                } else {
                    player.sendMessage(ChatColor.RED + "未找到对应 NPC。");
                }
                return true;
            }
            case "list" -> {
                player.sendMessage(ChatColor.YELLOW + "======= NPC 列表 =======");
                List<NpcDefinition> list = new ArrayList<>(npcService.list());
                if (list.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "暂无 NPC。");
                } else {
                    for (NpcDefinition definition : list) {
                        player.sendMessage(ChatColor.GOLD + definition.id() + ChatColor.WHITE + " - "
                                + ChatColor.AQUA + definition.behavior() + ChatColor.WHITE + " @ "
                                + definition.location().getWorld().getName() + " "
                                + definition.location().getBlockX() + ","
                                + definition.location().getBlockY() + ","
                                + definition.location().getBlockZ());
                    }
                }
                return true;
            }
            case "reload" -> {
                npcService.reload();
                player.sendMessage(ChatColor.GREEN + "NPC 已重新加载。");
                return true;
            }
            default -> {
                sendNpcHelp(player);
                return true;
            }
        }
    }

    private NpcBehavior parseBehavior(String input) {
        try {
            return NpcBehavior.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return NpcBehavior.SHOP;
        }
    }

    private EntityType parseEntityType(String input) {
        try {
            return EntityType.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return EntityType.VILLAGER;
        }
    }

    private boolean handleScripts(Player player, String[] args) {
        if (!player.hasPermission("dirtshop.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限管理脚本。");
            return true;
        }
        if (args.length < 2) {
            sendScriptHelp(player);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                scriptService.reloadScripts();
                player.sendMessage(ChatColor.GREEN + "脚本已重新加载。");
                return true;
            }
            case "list" -> {
                player.sendMessage(ChatColor.YELLOW + "======= 已加载脚本 =======");
                List<String> scripts = scriptService.getLoadedScripts();
                if (scripts.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "暂无脚本。");
                } else {
                    scripts.forEach(name -> player.sendMessage(ChatColor.GOLD + name));
                }
                return true;
            }
            default -> {
                sendScriptHelp(player);
                return true;
            }
        }
    }

    private boolean handleTools(Player player) {
        player.sendMessage(ChatColor.YELLOW + "======= 泥土工具强化 =======");
        var modifiers = toolModificationService.getAllModifiers();
        modifiers.forEach((material, modifier) -> {
            List<String> lore = toolModificationService.describeModifier(material);
            if (!lore.isEmpty()) {
                player.sendMessage(ChatColor.GREEN + material.name());
                lore.forEach(player::sendMessage);
            }
        });
        if (modifiers.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "当前未配置强化工具。");
        }
        return true;
    }

    private boolean handleStats(Player player) {
        PlayerStats stats = statsService.getStats(player.getUniqueId());
        player.sendMessage(ChatColor.AQUA + "======= 成长面板 =======");
        player.sendMessage(ChatColor.GOLD + "泥土币: " + ChatColor.WHITE + economyService.getBalance(player));
        player.sendMessage(ChatColor.GREEN + "农业经验: " + stats.getAgricultureExp());
        player.sendMessage(ChatColor.RED + "战斗经验: " + stats.getCombatExp());
        player.sendMessage(ChatColor.BLUE + "魔法经验: " + stats.getMagicExp());
        player.sendMessage(ChatColor.GRAY + "防御经验: " + stats.getDefenseExp());
        player.sendMessage(ChatColor.DARK_AQUA + "钓鱼经验: " + stats.getFishingExp());
        player.sendMessage(ChatColor.DARK_GREEN + "射击经验: " + stats.getArcheryExp());
        player.sendMessage(ChatColor.LIGHT_PURPLE + "魔法值: "
                + String.format(Locale.ROOT, "%.1f/%.1f", statsService.getCurrentMagic(player), statsService.getMaxMagic(player)));
        player.sendMessage(ChatColor.YELLOW + "健康等级: " + String.format(Locale.ROOT, "%.1f", statsService.getHealthLevel(player.getUniqueId())));
        player.sendMessage(ChatColor.YELLOW + "防御等级: " + String.format(Locale.ROOT, "%.1f", statsService.getDefenseLevel(player.getUniqueId())));
        player.sendMessage(ChatColor.YELLOW + "魔法等级: " + String.format(Locale.ROOT, "%.1f", statsService.getMagicLevel(player.getUniqueId())));
        return true;
    }

    private boolean handleArmory(Player player, String[] args) {
        if (args.length < 2) {
            listArmory(player);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            listArmory(player);
            return true;
        }
        if (sub.equals("give")) {
            if (!player.hasPermission("dirtshop.admin")) {
                player.sendMessage(ChatColor.RED + "你没有权限发放武器。");
                return true;
            }
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "用法: /dirtshop armory give <weapon|armor> <id> [玩家]");
                return true;
            }
            String type = args[2].toLowerCase(Locale.ROOT);
            String id = args[3].toLowerCase(Locale.ROOT);
            Player target = player;
            if (args.length >= 5) {
                Player found = Bukkit.getPlayerExact(args[4]);
                if (found != null) {
                    target = found;
                }
            }
            ItemStack stack = switch (type) {
                case "weapon" -> armoryService.createWeapon(id);
                case "armor" -> armoryService.createArmor(id);
                default -> null;
            };
            if (stack == null) {
                player.sendMessage(ChatColor.RED + "未找到该武器或护甲。");
                return true;
            }
            target.getInventory().addItem(stack);
            player.sendMessage(ChatColor.GREEN + "已发放 " + id + " 给 " + target.getName());
            return true;
        }
        listArmory(player);
        return true;
    }

    private void listArmory(Player player) {
        player.sendMessage(ChatColor.LIGHT_PURPLE + "======= 武器库 =======");
        if (armoryService.getWeaponIds().isEmpty() && armoryService.getArmorIds().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "尚未配置武器或护甲。");
            return;
        }
        armoryService.getWeaponIds().forEach(id -> player.sendMessage(ChatColor.GOLD + "武器: " + id));
        armoryService.getArmorIds().forEach(id -> player.sendMessage(ChatColor.AQUA + "护甲: " + id));
    }

    private boolean handleZones(Player player, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("regenerate")) {
            if (!player.hasPermission("dirtshop.admin")) {
                player.sendMessage(ChatColor.RED + "你没有权限重置交易区。");
                return true;
            }
            regionService.regenerateTradingZones();
            regionService.updateCommissions(economyHealthService.getShopkeeperTaxRate());
            player.sendMessage(ChatColor.GREEN + "交易区已经重新随机化。");
            return true;
        }
        player.sendMessage(ChatColor.YELLOW + "======= 随机交易区 =======");
        for (TradingZone zone : regionService.getTradingZones()) {
            player.sendMessage(ChatColor.GOLD + zone.getId() + ChatColor.WHITE + " 店主:" + zone.getShopkeeper()
                    + ChatColor.GRAY + " 半径:" + zone.getRadius() + " 税率:" + String.format(Locale.ROOT, "%.2f", zone.getCommission())
                    + ChatColor.DARK_GRAY + " 累计收益:" + zone.getEarnings());
        }
        regionService.findTradingZone(player.getLocation()).ifPresent(zone ->
                player.sendMessage(ChatColor.GREEN + "你当前位于 " + zone.getId() + " 的守护范围内。"));
        player.sendMessage(ChatColor.YELLOW + "当前市场倍率 x" + String.format(Locale.ROOT, "%.2f", economyHealthService.getInflationMultiplier()));
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "======= 泥土商店 =======");
        player.sendMessage(ChatColor.GOLD + "/dirtshop balance" + ChatColor.WHITE + " - 查看泥土币余额");
        player.sendMessage(ChatColor.GOLD + "/dirtshop shop" + ChatColor.WHITE + " - 打开泥土商店");
        player.sendMessage(ChatColor.GOLD + "/dirtshop market browse" + ChatColor.WHITE + " - 浏览玩家市场");
        player.sendMessage(ChatColor.GOLD + "/dirtshop market list <价格>" + ChatColor.WHITE + " - 上架手中物品");
        player.sendMessage(ChatColor.GOLD + "/dirtshop market buy <编号>" + ChatColor.WHITE + " - 购买玩家市场物品");
        player.sendMessage(ChatColor.GOLD + "/dirtshop install [半径]" + ChatColor.WHITE + " - (管理员) 初始化商店范围");
        if (player.hasPermission("dirtshop.admin")) {
            player.sendMessage(ChatColor.GOLD + "/dirtshop npc" + ChatColor.WHITE + " - 管理商店 NPC");
            player.sendMessage(ChatColor.GOLD + "/dirtshop scripts" + ChatColor.WHITE + " - 管理外部脚本");
        }
        player.sendMessage(ChatColor.GOLD + "/dirtshop tools" + ChatColor.WHITE + " - 查看强化工具说明");
        player.sendMessage(ChatColor.GOLD + "/dirtshop stats" + ChatColor.WHITE + " - 查看个人成长");
        player.sendMessage(ChatColor.GOLD + "/dirtshop zones" + ChatColor.WHITE + " - 查看随机交易区");
        player.sendMessage(ChatColor.GOLD + "/dirtshop armory" + ChatColor.WHITE + " - 浏览武器与护甲");
    }

    private void sendMarketHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "用法: /dirtshop market <browse|list|buy|cancel>");
    }

    private void sendNpcHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "用法: /dirtshop npc <create|remove|move|name|type|behavior|dialogue|list|reload> ...");
    }

    private void sendScriptHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "用法: /dirtshop scripts <reload|list>");
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("balance");
            completions.add("shop");
            completions.add("market");
            completions.add("tools");
            completions.add("stats");
            completions.add("armory");
            completions.add("zones");
            if (sender.hasPermission("dirtshop.admin")) {
                completions.add("region");
                completions.add("install");
                completions.add("npc");
                completions.add("scripts");
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("market")) {
            completions.add("browse");
            completions.add("list");
            completions.add("buy");
            completions.add("cancel");
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("region") && sender.hasPermission("dirtshop.admin")) {
            completions.add("set");
            completions.add("require");
            completions.add("info");
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("npc") && sender.hasPermission("dirtshop.admin")) {
            completions.add("create");
            completions.add("remove");
            completions.add("move");
            completions.add("name");
            completions.add("type");
            completions.add("behavior");
            completions.add("dialogue");
            completions.add("list");
            completions.add("reload");
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("scripts") && sender.hasPermission("dirtshop.admin")) {
            completions.add("reload");
            completions.add("list");
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("npc") && sender.hasPermission("dirtshop.admin")) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "remove", "move", "name", "type", "behavior", "dialogue" ->
                        completions.addAll(npcService.list().stream().map(NpcDefinition::id).toList());
            }
            return completions;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("behavior")) {
            for (NpcBehavior behavior : NpcBehavior.values()) {
                completions.add(behavior.name().toLowerCase(Locale.ROOT));
            }
            return completions;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("type")) {
            for (EntityType type : EntityType.values()) {
                if (type.isSpawnable() && type.isAlive()) {
                    completions.add(type.name().toLowerCase(Locale.ROOT));
                }
            }
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("region") && args[1].equalsIgnoreCase("require")) {
            completions.add("true");
            completions.add("false");
            return completions;
        }
        return completions;
    }
}
