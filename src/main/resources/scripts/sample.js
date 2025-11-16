// PrimeMcDirtShop JavaScript 扩展示例
// library 变量提供了常用的钩子注册入口，请根据需要拷贝此文件进行修改。

library.registerWelcomeHook(function(player) {
    player.sendMessage("§a欢迎来到泥土商店！通过挖泥土赚取泥土币吧。§r");
});

library.registerBlockRewardModifier(function(player, material, reward) {
    if (material.name().contains("DIRT")) {
        return reward + 1; // 对所有泥土类方块额外赠送 1 泥土币
    }
    return reward;
});

library.registerNpcInteractionHandler(function(player, npcId, entity) {
    if (npcId === "guide") {
        player.sendMessage("§6向导: §f使用 /dirtshop shop 打开默认商店，购买你需要的物品！");
        return true; // 返回 true 阻止默认行为
    }
    return false;
});

library.registerToolDecorator(function(player, itemStack) {
    if (itemStack.getType().name().endsWith("SHOVEL")) {
        var meta = itemStack.getItemMeta();
        meta.setLore(java.util.Arrays.asList("§7脚本强化: 挖掘加成"));
        itemStack.setItemMeta(meta);
    }
});
