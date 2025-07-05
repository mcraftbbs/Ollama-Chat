package com.ollamachat.command;

import com.ollamachat.core.Ollamachat;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.chat.ChatTriggerHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final ChatTriggerHandler chatTriggerHandler;

    public OllamaChatCommand(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatTriggerHandler = new ChatTriggerHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text(configManager.getMessage("usage-ollSnackChat", null), NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ollamachat.reload")) {
                sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                return true;
            }
            configManager.reloadConfigValues();
            sender.sendMessage(Component.text(configManager.getMessage("reload-success", null), NamedTextColor.GREEN));
            return true;
        } else if (args[0].equalsIgnoreCase("toggle") && args.length > 1) {
            if (!sender.hasPermission("ollamachat.toggle")) {
                sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                return true;
            }
            String aiName = args[1];
            if (aiName.equalsIgnoreCase("ollama")) {
                configManager.setOllamaEnabled(!configManager.isOllamaEnabled());
                sender.sendMessage(Component.text(configManager.getMessage(configManager.isOllamaEnabled() ? "ollama-enabled" : "ollama-disabled", null), NamedTextColor.GREEN));
            } else if (configManager.getOtherAIConfigs().containsKey(aiName)) {
                boolean newState = !configManager.getOtherAIEnabled().getOrDefault(aiName, false);
                configManager.getOtherAIEnabled().put(aiName, newState);
                sender.sendMessage(Component.text(configManager.getMessage(newState ? "toggle-enabled" : "toggle-disabled", Map.of("ai-name", aiName)), NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text(configManager.getMessage("invalid-ai-name", Map.of("ai-list", String.join(", ", configManager.getOtherAIConfigs().keySet()))), NamedTextColor.RED));
            }
            return true;
        } else if (args[0].equalsIgnoreCase("prompt") && args.length > 1) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("set") && args.length > 3) {
                if (!sender.hasPermission("ollamachat.prompt.set")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                String promptName = args[2];
                String promptContent = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                plugin.getConfig().set("prompts." + promptName, promptContent);
                plugin.saveConfig();
                configManager.getPrompts().put(promptName, promptContent);
                sender.sendMessage(Component.text(configManager.getMessage("prompt-set", Map.of("name", promptName)), NamedTextColor.GREEN));
                return true;
            } else if (subCommand.equals("delete") && args.length == 3) {
                if (!sender.hasPermission("ollamachat.prompt.delete")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
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
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-deleted", Map.of("name", promptName)), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-not-found", Map.of("name", promptName)), NamedTextColor.RED));
                }
                return true;
            } else if (subCommand.equals("list")) {
                if (!sender.hasPermission("ollamachat.prompt.list")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                if (configManager.getPrompts().isEmpty()) {
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-list-empty", null), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-list", Map.of("prompts", String.join(", ", configManager.getPrompts().keySet()))), NamedTextColor.GREEN));
                }
                if (!configManager.getDefaultPrompt().isEmpty() && configManager.getPrompts().containsKey(configManager.getDefaultPrompt())) {
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-default", Map.of("name", configManager.getDefaultPrompt())), NamedTextColor.GREEN));
                } else if (!configManager.getDefaultPrompt().isEmpty()) {
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-default-invalid", Map.of("name", configManager.getDefaultPrompt())), NamedTextColor.RED));
                }
                return true;
            } else if (subCommand.equals("select") && args.length == 3) {
                if (!sender.hasPermission("ollamachat.prompt.select")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                String promptName = args[2];
                if (configManager.getPrompts().containsKey(promptName)) {
                    plugin.getConfig().set("default-prompt", promptName);
                    plugin.saveConfig();
                    configManager.setDefaultPrompt(promptName);
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-selected", Map.of("name", promptName)), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(configManager.getMessage("prompt-not-found", Map.of("name", promptName)), NamedTextColor.RED));
                }
                return true;
            } else if (subCommand.equals("clear")) {
                if (!sender.hasPermission("ollamachat.prompt.select")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                plugin.getConfig().set("default-prompt", "");
                plugin.saveConfig();
                configManager.setDefaultPrompt("");
                sender.sendMessage(Component.text(configManager.getMessage("prompt-cleared", null), NamedTextColor.GREEN));
                return true;
            } else {
                sender.sendMessage(Component.text(configManager.getMessage("prompt-usage", null), NamedTextColor.YELLOW));
                return true;
            }
        } else if (args[0].equalsIgnoreCase("conversation") && args.length > 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text(configManager.getMessage("player-only", null), NamedTextColor.RED));
                return true;
            }
            Player player = (Player) sender;
            String subCommand = args[1].toLowerCase();
            String aiName = args.length > 2 ? args[2] : "ollama";
            if (!aiName.equals("ollama") && !configManager.getOtherAIConfigs().containsKey(aiName)) {
                sender.sendMessage(Component.text(configManager.getMessage("invalid-ai-name", Map.of("ai-list", String.join(", ", configManager.getOtherAIConfigs().keySet()))), NamedTextColor.RED));
                return true;
            }
            if (subCommand.equals("new") && args.length == 4) {
                if (!sender.hasPermission("ollamachat.conversation.new")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                String convName = args[3];
                String convId = plugin.getChatHistoryManager().createConversation(player.getUniqueId(), aiName, convName);
                configManager.getSelectedConversations().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(aiName, convName);
                sender.sendMessage(Component.text(configManager.getMessage("conversation-created", Map.of("name", convName, "ai-name", aiName)), NamedTextColor.GREEN));
                return true;
            } else if (subCommand.equals("select") && args.length == 4) {
                if (!sender.hasPermission("ollamachat.conversation.select")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                String convName = args[3];
                if (plugin.getChatHistoryManager().conversationExistsByName(player.getUniqueId(), aiName, convName)) {
                    configManager.getSelectedConversations().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(aiName, convName);
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-selected", Map.of("name", convName, "ai-name", aiName)), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-not-found", Map.of("name", convName)), NamedTextColor.RED));
                }
                return true;
            } else if (subCommand.equals("delete") && args.length == 4) {
                if (!sender.hasPermission("ollamachat.conversation.delete")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                String convName = args[3];
                String convId = plugin.getChatHistoryManager().getConversationId(player.getUniqueId(), aiName, convName);
                if (convId != null && plugin.getChatHistoryManager().deleteConversation(player.getUniqueId(), aiName, convId)) {
                    Map<String, String> convMap = configManager.getSelectedConversations().get(player.getUniqueId());
                    if (convMap != null && convName.equals(convMap.get(aiName))) {
                        convMap.remove(aiName);
                    }
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-deleted", Map.of("name", convName, "ai-name", aiName)), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-not-found", Map.of("name", convName)), NamedTextColor.RED));
                }
                return true;
            } else if (subCommand.equals("list") && args.length == 3) {
                if (!sender.hasPermission("ollamachat.conversation.list")) {
                    sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                    return true;
                }
                Map<String, String> conversations = plugin.getChatHistoryManager().listConversations(player.getUniqueId(), aiName);
                if (conversations.isEmpty()) {
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-list-empty", Map.of("ai-name", aiName)), NamedTextColor.GREEN));
                } else {
                    String convList = String.join(", ", conversations.values());
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-list", Map.of("conversations", convList, "ai-name", aiName)), NamedTextColor.GREEN));
                }
                String selectedConv = configManager.getSelectedConversations().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).get(aiName);
                if (selectedConv != null && conversations.containsValue(selectedConv)) {
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-default", Map.of("name", selectedConv, "ai-name", aiName)), NamedTextColor.GREEN));
                } else if (selectedConv != null) {
                    sender.sendMessage(Component.text(configManager.getMessage("conversation-default-invalid", Map.of("name", selectedConv, "ai-name", aiName)), NamedTextColor.RED));
                }
                return true;
            } else {
                sender.sendMessage(Component.text(configManager.getMessage("conversation-usage", null), NamedTextColor.YELLOW));
                return true;
            }
        } else if (args[0].equalsIgnoreCase("suggests") && args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text(configManager.getMessage("player-only", null), NamedTextColor.RED));
                return true;
            }
            if (!sender.hasPermission("ollamachat.suggests.toggle")) {
                sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                return true;
            }
            Player player = (Player) sender;
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("on")) {
                plugin.getSuggestedResponseHandler().toggleSuggestionsForPlayer(player, true);
                sender.sendMessage(Component.text(configManager.getMessage("suggests-enabled", Map.of("player", player.getName())), NamedTextColor.GREEN));
                return true;
            } else if (subCommand.equals("off")) {
                plugin.getSuggestedResponseHandler().toggleSuggestionsForPlayer(player, false);
                sender.sendMessage(Component.text(configManager.getMessage("suggests-disabled", Map.of("player", player.getName())), NamedTextColor.RED));
                return true;
            } else {
                sender.sendMessage(Component.text(configManager.getMessage("usage-ollamachat", null), NamedTextColor.YELLOW));
                return true;
            }
        } else if (args[0].equalsIgnoreCase("suggests-presets") && args.length == 2) {
            if (!sender.hasPermission("ollamachat.suggests-presets.toggle")) {
                sender.sendMessage(Component.text(configManager.getMessage("no-permission", null), NamedTextColor.RED));
                return true;
            }
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("on")) {
                configManager.setSuggestedResponsePresetsEnabled(true);
                sender.sendMessage(Component.text(configManager.getMessage("suggests-presets-enabled", null), NamedTextColor.GREEN));
                return true;
            } else if (subCommand.equals("off")) {
                configManager.setSuggestedResponsePresetsEnabled(false);
                sender.sendMessage(Component.text(configManager.getMessage("suggests-presets-disabled", null), NamedTextColor.RED));
                return true;
            } else {
                sender.sendMessage(Component.text(configManager.getMessage("usage-ollamachat", null), NamedTextColor.YELLOW));
                return true;
            }
        }
        sender.sendMessage(Component.text(configManager.getMessage("usage-ollamachat", null), NamedTextColor.YELLOW));
        return true;
    }
}






