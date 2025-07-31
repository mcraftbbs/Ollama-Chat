package com.ollamachat.command;

import com.ollamachat.core.Ollamachat;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.chat.ChatTriggerHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public class AIChatCommand implements CommandExecutor {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final ChatTriggerHandler chatTriggerHandler;

    public AIChatCommand(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatTriggerHandler = new ChatTriggerHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage-aichat", null));
            return true;
        }

        String aiName = args[0];
        String prompt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (aiName.equalsIgnoreCase("ollama") || configManager.getOtherAIConfigs().containsKey(aiName)) {
            if (sender instanceof Player) {
                chatTriggerHandler.processAIQuery((Player) sender, aiName, prompt);
            } else {
                sender.sendMessage(configManager.getMessage("player-only", null));
            }
        } else {
            sender.sendMessage(configManager.getMessage("invalid-ai-name", Map.of("ai-list", String.join(", ", configManager.getOtherAIConfigs().keySet()))));
        }
        return true;
    }
}