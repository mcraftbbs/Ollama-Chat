package com.ollamachat.command;

import com.ollamachat.core.Ollamachat;
import com.ollamachat.core.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OllamaChatCommand implements CommandExecutor {
    private final Ollamachat plugin;
    private final ConfigManager configManager;

    public OllamaChatCommand(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getMessage("usage-ollamachat", null));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ollamachat.reload")) {
                sender.sendMessage(configManager.getMessage("no-permission", null));
                return true;
            }
            configManager.reloadConfigValues();
            sender.sendMessage(configManager.getMessage("reload-success", null));
            return true;
        } else if (args[0].equalsIgnoreCase("toggle") && args.length > 1) {
            if (!sender.hasPermission("ollamachat.toggle")) {
                sender.sendMessage(configManager.getMessage("no-permission", null));
                return true;
            }
            String aiName = args[1];
            if (aiName.equalsIgnoreCase("ollama")) {
                configManager.setOllamaEnabled(!configManager.isOllamaEnabled());
                sender.sendMessage(configManager.getMessage(configManager.isOllamaEnabled() ? "ollama-enabled" : "ollama-disabled", null));
            } else if (configManager.getOtherAIConfigs().containsKey(aiName)) {
                boolean newState = !configManager.getOtherAIEnabled().getOrDefault(aiName, false);
                configManager.getOtherAIEnabled().put(aiName, newState);
                sender.sendMessage(configManager.getMessage(newState ? "toggle-enabled" : "toggle-disabled", Map.of("ai-name", aiName)));
            } else {
                sender.sendMessage(configManager.getMessage("invalid-ai-name", Map.of("ai-list", String.join(", ", configManager.getOtherAIConfigs().keySet()))));
            }
            return true;
        } else if (args[0].equalsIgnoreCase("prompt") && args.length > 1) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("set") && args.length > 3) {
                if (!sender.hasPermission("ollamachat.prompt.set")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                String promptName = args[2];
                String promptContent = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                plugin.getConfig().set("prompts." + promptName, promptContent);
                plugin.saveConfig();
                configManager.getPrompts().put(promptName, promptContent);
                sender.sendMessage(configManager.getMessage("prompt-set", Map.of("name", promptName)));
                return true;
            } else if (subCommand.equals("delete") && args.length == 3) {
                if (!sender.hasPermission("ollamachat.prompt.delete")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                String promptName = args[2];
                if (configManager.getPrompts().containsKey(promptName)) {
                    plugin.getConfig().set("prompts." + promptName, null);
                    if (configManager.getDefaultPrompt().equals(promptName)) {
                        plugin.getConfig().set("default-prompt", "");
                        configManager.setDefaultPrompt("");
                    }
                    plugin.saveConfig();
                    configManager.getPrompts().remove(promptName);
                    sender.sendMessage(configManager.getMessage("prompt-deleted", Map.of("name", promptName)));
                } else {
                    sender.sendMessage(configManager.getMessage("prompt-not-found", Map.of("name", promptName)));
                }
                return true;
            } else if (subCommand.equals("list")) {
                if (!sender.hasPermission("ollamachat.prompt.list")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                if (configManager.getPrompts().isEmpty()) {
                    sender.sendMessage(configManager.getMessage("prompt-list-empty", null));
                } else {
                    sender.sendMessage(configManager.getMessage("prompt-list", Map.of("prompts", String.join(", ", configManager.getPrompts().keySet()))));
                }
                if (!configManager.getDefaultPrompt().isEmpty() && configManager.getPrompts().containsKey(configManager.getDefaultPrompt())) {
                    sender.sendMessage(configManager.getMessage("prompt-default", Map.of("name", configManager.getDefaultPrompt())));
                } else if (!configManager.getDefaultPrompt().isEmpty()) {
                    sender.sendMessage(configManager.getMessage("prompt-default-invalid", Map.of("name", configManager.getDefaultPrompt())));
                }
                return true;
            } else if (subCommand.equals("select") && args.length == 3) {
                if (!sender.hasPermission("ollamachat.prompt.select")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                String promptName = args[2];
                if (configManager.getPrompts().containsKey(promptName)) {
                    plugin.getConfig().set("default-prompt", promptName);
                    plugin.saveConfig();
                    configManager.setDefaultPrompt(promptName);
                    sender.sendMessage(configManager.getMessage("prompt-selected", Map.of("name", promptName)));
                } else {
                    sender.sendMessage(configManager.getMessage("prompt-not-found", Map.of("name", promptName)));
                }
                return true;
            } else if (subCommand.equals("clear")) {
                if (!sender.hasPermission("ollamachat.prompt.select")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                plugin.getConfig().set("default-prompt", "");
                plugin.saveConfig();
                configManager.setDefaultPrompt("");
                sender.sendMessage(configManager.getMessage("prompt-cleared", null));
                return true;
            } else {
                sender.sendMessage(configManager.getMessage("prompt-usage", null));
                return true;
            }
        } else if (args[0].equalsIgnoreCase("conversation") && args.length > 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getMessage("player-only", null));
                return true;
            }
            Player player = (Player) sender;
            String subCommand = args[1].toLowerCase();
            String aiName = args.length > 2 ? args[2] : "ollama";
            if (!aiName.equals("ollama") && !configManager.getOtherAIConfigs().containsKey(aiName)) {
                sender.sendMessage(configManager.getMessage("invalid-ai-name", Map.of("ai-list", String.join(", ", configManager.getOtherAIConfigs().keySet()))));
                return true;
            }
            if (subCommand.equals("new") && args.length == 4) {
                if (!sender.hasPermission("ollamachat.conversation.new")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                String convName = args[3];
                String convId = plugin.getChatHistoryManager().createConversation(player.getUniqueId(), aiName, convName);
                configManager.getSelectedConversations().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(aiName, convName);
                sender.sendMessage(configManager.getMessage("conversation-created", Map.of("name", convName, "ai-name", aiName)));
                return true;
            } else if (subCommand.equals("select") && args.length == 4) {
                if (!sender.hasPermission("ollamachat.conversation.select")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                String convName = args[3];
                if (plugin.getChatHistoryManager().conversationExistsByName(player.getUniqueId(), aiName, convName)) {
                    configManager.getSelectedConversations().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(aiName, convName);
                    sender.sendMessage(configManager.getMessage("conversation-selected", Map.of("name", convName, "ai-name", aiName)));
                } else {
                    sender.sendMessage(configManager.getMessage("conversation-not-found", Map.of("name", convName)));
                }
                return true;
            } else if (subCommand.equals("delete") && args.length == 4) {
                if (!sender.hasPermission("ollamachat.conversation.delete")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                String convName = args[3];
                String convId = plugin.getChatHistoryManager().getConversationId(player.getUniqueId(), aiName, convName);
                if (convId != null && plugin.getChatHistoryManager().deleteConversation(player.getUniqueId(), aiName, convId)) {
                    Map<String, String> convMap = configManager.getSelectedConversations().get(player.getUniqueId());
                    if (convMap != null && convName.equals(convMap.get(aiName))) {
                        convMap.remove(aiName);
                    }
                    sender.sendMessage(configManager.getMessage("conversation-deleted", Map.of("name", convName, "ai-name", aiName)));
                } else {
                    sender.sendMessage(configManager.getMessage("conversation-not-found", Map.of("name", convName)));
                }
                return true;
            } else if (subCommand.equals("list") && args.length == 3) {
                if (!sender.hasPermission("ollamachat.conversation.list")) {
                    sender.sendMessage(configManager.getMessage("no-permission", null));
                    return true;
                }
                Map<String, String> conversations = plugin.getChatHistoryManager().listConversations(player.getUniqueId(), aiName);
                if (conversations.isEmpty()) {
                    sender.sendMessage(configManager.getMessage("conversation-list-empty", Map.of("ai-name", aiName)));
                } else {
                    String convList = String.join(", ", conversations.values());
                    sender.sendMessage(configManager.getMessage("conversation-list", Map.of("conversations", convList, "ai-name", aiName)));
                }
                String selectedConv = configManager.getSelectedConversations().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).get(aiName);
                if (selectedConv != null && conversations.containsValue(selectedConv)) {
                    sender.sendMessage(configManager.getMessage("conversation-default", Map.of("name", selectedConv, "ai-name", aiName)));
                } else if (selectedConv != null) {
                    sender.sendMessage(configManager.getMessage("conversation-default-invalid", Map.of("name", selectedConv, "ai-name", aiName)));
                }
                return true;
            } else {
                sender.sendMessage(configManager.getMessage("conversation-usage", null));
                return true;
            }
        }
        return false;
    }
}


