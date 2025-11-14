# PrimeMcDirtShop

PrimeMcDirtShop 是一款针对 Bukkit/Spigot/Paper 1.20+ 的泥土商店插件。玩家可以通过挖掘泥土获取“泥土币”作为服务器内通用货币，使用图形化商店购买物资，或者在玩家市场中自由交易。插件内部集成 Spring ApplicationContext 并手动注册 Bean，结构清晰易于扩展。

## 功能概览

- **泥土币经济体系**：支持多种泥土相关方块，自定义掉落数值，可选的时运加成。
- **图形化泥土商店**：通过 `/dirtshop shop` 打开 GUI 购买服务器预设物品，覆盖主流物品类别。
- **玩家市场**：玩家可以在商店范围内上架、浏览、购买和撤销自定义商品。
- **NPC 管理系统**：内置商贩、市场经纪人等 NPC，支持通过命令创建、移动、改名、配置对话及行为。
- **原版工具强化**：启动时为主流工具批量应用名称、Lore、附魔和耐久倍率，保障泥土经济体验。
- **外置 JavaScript 扩展**：在 `plugins/PrimeMcDirtShop/scripts/` 目录编写 .js 脚本，即可挂载欢迎消息、奖励倍率、NPC 互动、工具装饰等逻辑。
- **欢迎消息广播**：玩家加入时显示标题与多行提示，可通过配置或脚本自定义。
- **商店安装与区域保护**：`/dirtshop install` 命令可快速圈定商店中心及半径，必要时强制玩家在区域内进行交易。
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
| `/dirtshop npc ...` | （管理员）管理泥土商店 NPC（创建、移动、改名、行为、对话、列表、重载） |
| `/dirtshop scripts <reload|list>` | （管理员）重载或查看已加载的 JavaScript 脚本 |

## 配置文件概述

- `welcome`：定义玩家加入服务器时的标题、子标题以及多行欢迎消息。
- `dirt-currency`：定义哪些方块能够掉落泥土币以及对应数值，可开启或关闭时运加成。
- `tool-modifiers`：为主流工具指定名称、Lore、附魔、耐久倍率（支持脚本再次装饰）。
- `shop.trades`：预设商店的出售商品，覆盖建筑、矿物、食物、装备等近 70 种物品。
- `market`：限制每位玩家的上架数量，以及商品自动过期时间（小时）。
- `region`：记录商店中心、半径及是否强制在范围内交易；可通过命令实时更新。
- `npcs`：存储 NPC 的世界坐标、行为、显示名称和对话文本，可通过命令写回配置。

## 数据文件

- `player-balances.yml`：存储玩家泥土币余额。
- `market-listings.yml`：存储玩家市场当前上架商品。
- `scripts/` 目录：存放可热加载的 JavaScript 脚本，插件已附带 `sample.js` 作为扩展示例。

如需进行深度定制，可在 `src/main/java/com/primemcdirtshop/dirtshop/` 中查看 Spring Bean 定义与服务实现。欢迎根据服务器需求二次开发！
