# PrimeMcDirtShop

PrimeMcDirtShop 是一款针对 Bukkit/Spigot/Paper 1.20+ 的泥土商店插件。玩家可以通过挖掘泥土获取“泥土币”作为服务器内通用货币，使用图形化商店购买物资，或者在玩家市场中自由交易。插件内部集成 Spring ApplicationContext 并手动注册 Bean，结构清晰易于扩展。

## 功能概览

- **泥土币经济体系**：支持多种泥土相关方块，自定义掉落数值，可选的时运加成。
- **图形化泥土商店**：通过 `/dirtshop shop` 打开 GUI 购买服务器预设物品，覆盖主流物品类别。
- **玩家市场**：玩家可以在商店范围内上架、浏览、购买和撤销自定义商品，并根据全服财富动态调整价格与抽成比例。
- **NPC 管理系统**：内置商贩、市场经纪人等 NPC，支持通过命令创建、移动、改名、配置对话及行为。
- **原版工具强化**：启动时为主流工具批量应用名称、Lore、附魔和耐久倍率，保障泥土经济体验。
- **外置 JavaScript 扩展**：在 `plugins/PrimeMcDirtShop/scripts/` 目录编写 .js 脚本，即可挂载欢迎消息、奖励倍率、NPC 互动、工具装饰等逻辑。
- **欢迎消息广播**：玩家加入时显示标题与多行提示，可通过配置或脚本自定义。
- **商店安装与区域保护**：`/dirtshop install` 命令可快速圈定商店中心及半径，必要时强制玩家在区域内进行交易。
- **交易区域与 VIP**：`primemc.perm.pro` 或 `primemc.perm.ultra` 玩家可随地交易，普通玩家需在主区域或随机生成的交易区内完成市场操作。
- **店主收益机制**：每个交易区域都有店主（或 NPC）管理，服务器会根据全服交易量自动在 20%~25% 间浮动抽成并累计收益。
- **武器与护甲库**：配置文件内置多把技能武器、特效护甲，可通过 `/dirtshop arsenal` 查看或发放，并带来粒子效果及技能冷却。
- **成长系统**：新增血量、魔法、 防御、农业、战斗、钓鱼、射击等经验类型，随着日常玩法自动升级，直接影响玩家最大生命、魔力容量与减伤。
- **Spring 管理**：插件启动时构建 Spring ApplicationContext，统一管理经济、商店、市场、NPC、脚本等服务，方便日后扩展。

## 构建与安装

1. 安装 JDK 17 及 Maven。
2. 在项目根目录执行 `mvn package`，生成的插件位于 `target/PrimeMcDirtShop-1.0.0-shaded.jar`。
3. 将生成的 JAR 文件拷贝至服务器 `plugins/` 目录并启动服务器。
4. 首次启动后会生成默认配置 `config.yml`，可按需修改商店商品、市场上限、泥土币值等。
5. 使用管理员账号登录服务器，执行 `/dirtshop install <半径>`（不填默认为 10）来设置商店中心，同时开启区域限制。

## 核心命令

| 命令 | 描述 |
| ---- | ---- |
| `/dirtshop balance` | 查看自己的泥土币余额 |
| `/dirtshop shop` | 打开泥土商店 GUI |
| `/dirtshop market browse` | 浏览玩家市场在售物品 |
| `/dirtshop market list <价格>` | 在商店区域内上架手中物品 |
| `/dirtshop market buy <编号>` | 购买指定编号的市场商品 |
| `/dirtshop market cancel <编号>` | 撤销自己的上架物品 |
| `/dirtshop tools` | 查看当前强化工具的效果说明 |
| `/dirtshop install [半径]` | （管理员）快速设置商店中心并启用区域限制 |
| `/dirtshop region set <半径>` | （管理员）手动调整商店半径 |
| `/dirtshop region require <true|false>` | （管理员）开启或关闭区域限制 |
| `/dirtshop region info` | （管理员）查看当前商店位置参数 |
| `/dirtshop region random` | （管理员）使用算法在配置世界随机生成一个交易区域 |
| `/dirtshop region zones` | （管理员）列出已存在的动态交易区域及收益 |
| `/dirtshop npc ...` | （管理员）管理泥土商店 NPC（创建、移动、改名、行为、对话、列表、重载） |
| `/dirtshop scripts <reload|list>` | （管理员）重载或查看已加载的 JavaScript 脚本 |
| `/dirtshop stats` | 查看个人血量/魔法/经验等成长属性 |
| `/dirtshop arsenal <list|give>` | 查看预置的武器与护甲，或（管理员）发放指定装备 |

## 配置文件概述

- `welcome`：定义玩家加入服务器时的标题、子标题以及多行欢迎消息。
- `dirt-currency`：定义哪些方块能够掉落泥土币以及对应数值，可开启或关闭时运加成。
- `tool-modifiers`：为主流工具指定名称、Lore、附魔、耐久倍率（支持脚本再次装饰）。
- `shop.trades`：预设商店的出售商品，覆盖建筑、矿物、食物、装备等近 70 种物品。
- `market`：限制每位玩家的上架数量、商品自动过期时间，以及由全服财富驱动的动态溢价（自动计算）。
- `region`：记录商店中心、半径、店主称号和是否强制在范围内交易；同时包含 `generator` 配置，用于随机生成多个交易区域。
- `arsenal`：预定义武器与护甲的材质、名称、Lore、技能、粒子以及魔法/防御参数，可直接扩展自定义武器库。
- `npcs`：存储 NPC 的世界坐标、行为、显示名称和对话文本，可通过命令写回配置。

## 数据文件

- `player-balances.yml`：存储玩家泥土币余额。
- `market-listings.yml`：存储玩家市场当前上架商品。
- `player-stats.yml`：存储每位玩家的血量、魔法、防御和各类生活/战斗经验，用于跨服重启后的成长继承。
- `scripts/` 目录：存放可热加载的 JavaScript 脚本，插件已附带 `sample.js` 作为扩展示例。

## 战斗与成长机制

- 挖掘农作物、钓鱼、战斗击杀、射箭、附魔、购买护甲等行为都会自动累计对应经验值。
- 血量经验会提升最大生命值，魔法经验可增加技能消耗上限，防御经验和护甲装备则直接影响受到的伤害。
- `/dirtshop stats` 命令可实时查看当前经验、等级与魔力值，方便玩家规划成长路线。
- `/dirtshop arsenal` 命令提供了多把带技能的武器（旋风、奥术冲击、神射聚焦等）以及护甲，每件装备都伴随粒子特效，且可通过配置文件自由增删。

## 动态交易区域与店主抽成

- 管理员可使用 `/dirtshop region random` 调用内置算法，在配置的世界坐标范围内寻找安全地点并生成新的交易区域；结果将写回 `config.yml` 的 `region.dynamic-zones`。
- 每个区域都绑定店主昵称，并会根据全服财富与交易总量自动计算 20%~25% 的抽成比例。抽成通过聊天提示反馈给玩家，同时写入配置以便统计收益。
- 拥有 `primemc.perm.pro` 或 `primemc.perm.ultra` 的 VIP 玩家可以无视区域限制在任意位置使用市场功能，其他玩家需在主区域或动态区域内完成交易。

## JavaScript 扩展开发教程

PrimeMcDirtShop 使用 Nashorn 引擎执行 `plugins/PrimeMcDirtShop/scripts/` 目录下的 `.js` 文件，并在每次 `/dirtshop scripts reload` 时重新加载。【F:src/main/java/com/primemcdirtshop/dirtshop/scripting/ScriptService.java†L46-L107】以下内容帮助你快速上手：

1. **准备环境**
   - 首次启动插件会自动创建 `scripts` 目录及 `sample.js` 示例脚本。
   - 若需在脚本中使用额外 Java 库，请确保相关 JAR 已被服务器加载（例如放入 `libs/` 并由启动脚本添加到 ClassPath），这样才能通过 `Java.type` 调用。

2. **了解可用绑定**
   - 每个脚本会获得以下变量：
     - `library`：封装常用钩子的帮助器，可注册欢迎消息、挖掘奖励、工具装饰、NPC 互动等函数。【F:src/main/java/com/primemcdirtshop/dirtshop/scripting/DirtScriptLibrary.java†L27-L88】
     - `plugin`：`JavaPlugin` 实例，可用于调度任务、访问数据目录等。
     - `economy`：`DirtEconomyService`，可查询或修改玩家泥土币余额。
     - `configuration`：`PluginConfiguration`，可读取插件的运行配置。

3. **注册钩子**
   - `library.registerWelcomeHook(player => { ... })`：玩家加入时执行，可发送消息或发放奖励。
   - `library.registerBlockRewardModifier((player, material, reward) => { ... return reward; })`：自定义挖掘泥土时的奖励倍率或额外掉落。
   - `library.registerToolDecorator((player, itemStack) => { ... })`：在工具加载或玩家背包刷新时追加 Lore、附魔等效果。
   - `library.registerNpcInteractionHandler((player, npcId, entity) => { ... return handled; })`：拦截 NPC 交互事件，返回 `true` 则阻止默认逻辑。
   - `library.onEnable(plugin => { ... })`：脚本加载后立即调用，常用于调度定时任务或缓存数据。

4. **经济与工具接口**
   - `library.economy()`/`economy` 暴露的 `getBalance`、`deposit`、`withdraw` 等方法，可直接管理玩家泥土币余额。
   - `library.tools()` 暴露的 `applyAllModifiers(Player)` 等方法，可复用插件内置的工具强化逻辑。【F:src/main/java/com/primemcdirtshop/dirtshop/scripting/DirtScriptLibrary.java†L63-L79】

5. **调试与热重载**
   - 修改脚本后执行 `/dirtshop scripts reload` 即可热重载；插件会记录已加载脚本并输出成功或错误信息，便于定位问题。【F:src/main/java/com/primemcdirtshop/dirtshop/scripting/ScriptService.java†L69-L106】
   - 在脚本中使用 `plugin.getLogger().info("...")` 或 `print("...")` 输出调试信息。

6. **最佳实践**
   - 使用 `try { ... } catch (e) { plugin.getLogger().log(...) }` 包裹可能抛错的逻辑，避免单个脚本影响所有钩子。
   - 将复杂逻辑拆分为多个函数，并在脚本底部调用 `library` 注册，确保可读性。
   - 利用示例脚本 `sample.js` 作为模板，自定义对话、奖励或工具装饰。

完成以上步骤后，即可通过 JavaScript 脚本无缝扩展泥土商店的欢迎流程、经济奖励、NPC 行为以及工具强化等玩法。

如需进行深度定制，可在 `src/main/java/com/primemcdirtshop/dirtshop/` 中查看 Spring Bean 定义与服务实现。欢迎根据服务器需求二次开发！
