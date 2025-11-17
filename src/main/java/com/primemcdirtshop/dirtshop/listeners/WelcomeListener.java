package com.primemcdirtshop.dirtshop.listeners;

import com.primemcdirtshop.dirtshop.config.PluginConfiguration;
import com.primemcdirtshop.dirtshop.config.WelcomeSettings;
import com.primemcdirtshop.dirtshop.scripting.ScriptService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;

public class WelcomeListener implements Listener {

    private final PluginConfiguration configuration;
    private final ScriptService scriptService;

    public WelcomeListener(PluginConfiguration configuration, ScriptService scriptService) {
        this.configuration = configuration;
        this.scriptService = scriptService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        WelcomeSettings welcome = configuration.getWelcomeSettings();
        if (welcome.enabled()) {
            sendMessages(player, welcome);
        }
        scriptService.fireWelcome(player);
    }

    private void sendMessages(Player player, WelcomeSettings welcome) {
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
        if (welcome.title() != null || welcome.subtitle() != null) {
            Component title = welcome.title() != null
                    ? serializer.deserialize(welcome.title())
                    : Component.empty();
            Component subtitle = welcome.subtitle() != null
                    ? serializer.deserialize(welcome.subtitle())
                    : Component.empty();
            player.showTitle(Title.title(title, subtitle, Title.Times.times(
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(3),
                    Duration.ofSeconds(1)
            )));
        }
        for (String line : welcome.messages()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }
}
