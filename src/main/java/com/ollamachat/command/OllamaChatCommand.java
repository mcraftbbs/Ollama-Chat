package com.ollamachat.command;

import com.ollamachat.core.Ollamachat;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.WebSearchService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("usage-ollamachat", null));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "toggle":
                return handleToggle(sender, args);
            case "prompt":
                return handlePrompt(sender, args);
            case "conversation":
                return handleConversation(sender, args);
            case "suggests":
                return handleSuggests(sender, args);
            case "suggests-presets":
                return handleSuggestsPresets(sender, args);
            case "search":
                return handleSearch(sender, args);
            default:
                sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("usage-ollamachat", null));
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ollamachat.reload")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        try {
            //
            plugin.reloadConfig();
            configManager.reloadConfigValues();
            //
            configManager.reloadLanguage();

            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("reload-success", null));
            plugin.getLogger().info("Configuration reloaded successfully by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("general-error",
                    Map.of("error", e.getMessage())));
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.toggle")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("usage-ollamachat", null));
            return true;
        }

        String aiName = args[1];
        if (aiName.equalsIgnoreCase("ollama")) {
            configManager.setOllamaEnabled(!configManager.isOllamaEnabled());
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage(
                    configManager.isOllamaEnabled() ? "ollama-enabled" : "ollama-disabled", null));
        } else if (configManager.getOtherAIConfigs().containsKey(aiName)) {
            boolean newState = !configManager.getOtherAIEnabled().getOrDefault(aiName, false);
            configManager.getOtherAIEnabled().put(aiName, newState);
            plugin.getConfig().set("other-ai-configs." + aiName + ".enabled", newState);
            plugin.saveConfig();
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage(
                    newState ? "toggle-enabled" : "toggle-disabled", Map.of("ai-name", aiName)));
        } else {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("invalid-ai-name",
                    Map.of("ai-list", String.join(", ", configManager.getOtherAIConfigs().keySet()))));
        }
        return true;
    }

    private boolean handlePrompt(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("prompt-usage", null));
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "set":
                return handlePromptSet(sender, args);
            case "delete":
                return handlePromptDelete(sender, args);
            case "list":
                return handlePromptList(sender);
            case "select":
                return handlePromptSelect(sender, args);
            case "clear":
                return handlePromptClear(sender);
            default:
                sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("prompt-usage", null));
                return true;
        }
    }

    private boolean handlePromptSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.prompt.set")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("prompt-usage", null));
            return true;
        }

        String promptName = args[2];
        String promptContent = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        configManager.addPrompt(promptName, promptContent);
        sender.sendMessage(ChatColor.GREEN + configManager.getMessage("prompt-set",
                Map.of("name", promptName)));
        return true;
    }

    private boolean handlePromptDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.prompt.delete")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("prompt-usage", null));
            return true;
        }

        String promptName = args[2];
        if (configManager.getPrompts().containsKey(promptName)) {
            configManager.removePrompt(promptName);
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("prompt-deleted",
                    Map.of("name", promptName)));
        } else {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("prompt-not-found",
                    Map.of("name", promptName)));
        }
        return true;
    }

    private boolean handlePromptList(CommandSender sender) {
        if (!sender.hasPermission("ollamachat.prompt.list")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (configManager.getPrompts().isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("prompt-list-empty", null));
        } else {
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("prompt-list",
                    Map.of("prompts", String.join(", ", configManager.getPrompts().keySet()))));
        }

        if (!configManager.getDefaultPrompt().isEmpty()) {
            if (configManager.getPrompts().containsKey(configManager.getDefaultPrompt())) {
                sender.sendMessage(ChatColor.GREEN + configManager.getMessage("prompt-default",
                        Map.of("name", configManager.getDefaultPrompt())));
            } else {
                sender.sendMessage(ChatColor.RED + configManager.getMessage("prompt-default-invalid",
                        Map.of("name", configManager.getDefaultPrompt())));
            }
        }
        return true;
    }

    private boolean handlePromptSelect(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.prompt.select")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("prompt-usage", null));
            return true;
        }

        String promptName = args[2];
        if (configManager.getPrompts().containsKey(promptName)) {
            configManager.setDefaultPrompt(promptName);
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("prompt-selected",
                    Map.of("name", promptName)));
        } else {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("prompt-not-found",
                    Map.of("name", promptName)));
        }
        return true;
    }

    private boolean handlePromptClear(CommandSender sender) {
        if (!sender.hasPermission("ollamachat.prompt.select")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        configManager.setDefaultPrompt("");
        sender.sendMessage(ChatColor.GREEN + configManager.getMessage("prompt-cleared", null));
        return true;
    }

    private boolean handleConversation(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("player-only", null));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("conversation-usage", null));
            return true;
        }

        Player player = (Player) sender;
        String subCommand = args[1].toLowerCase();
        String aiName = args[2];

        if (!aiName.equals("ollama") && !configManager.getOtherAIConfigs().containsKey(aiName)) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("invalid-ai-name",
                    Map.of("ai-list", String.join(", ", configManager.getOtherAIConfigs().keySet()))));
            return true;
        }

        switch (subCommand) {
            case "new":
                return handleConversationNew(player, aiName, args);
            case "select":
                return handleConversationSelect(player, aiName, args);
            case "delete":
                return handleConversationDelete(player, aiName, args);
            case "list":
                return handleConversationList(player, aiName);
            default:
                sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("conversation-usage", null));
                return true;
        }
    }

    private boolean handleConversationNew(Player player, String aiName, String[] args) {
        if (!player.hasPermission("ollamachat.conversation.new")) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("conversation-usage", null));
            return true;
        }

        String convName = args[3];

        if (plugin.getChatHistoryManager().conversationExistsByName(player.getUniqueId(), aiName, convName)) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("conversation-not-found",
                    Map.of("name", convName)));
            return true;
        }

        String convId = plugin.getChatHistoryManager().createConversation(player.getUniqueId(), aiName, convName);
        if (convId != null) {
            configManager.getSelectedConversations()
                    .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(aiName, convName);
            player.sendMessage(ChatColor.GREEN + configManager.getMessage("conversation-created",
                    Map.of("name", convName, "ai-name", aiName)));
        } else {
            player.sendMessage(ChatColor.RED + configManager.getMessage("database-error",
                    Map.of("error", "Failed to create conversation")));
        }
        return true;
    }

    private boolean handleConversationSelect(Player player, String aiName, String[] args) {
        if (!player.hasPermission("ollamachat.conversation.select")) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("conversation-usage", null));
            return true;
        }

        String convName = args[3];
        if (plugin.getChatHistoryManager().conversationExistsByName(player.getUniqueId(), aiName, convName)) {
            configManager.getSelectedConversations()
                    .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(aiName, convName);
            player.sendMessage(ChatColor.GREEN + configManager.getMessage("conversation-selected",
                    Map.of("name", convName, "ai-name", aiName)));
        } else {
            player.sendMessage(ChatColor.RED + configManager.getMessage("conversation-not-found",
                    Map.of("name", convName)));
        }
        return true;
    }

    private boolean handleConversationDelete(Player player, String aiName, String[] args) {
        if (!player.hasPermission("ollamachat.conversation.delete")) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("conversation-usage", null));
            return true;
        }

        String convName = args[3];
        String convId = plugin.getChatHistoryManager().getConversationId(player.getUniqueId(), aiName, convName);

        if (convId != null && plugin.getChatHistoryManager().deleteConversation(player.getUniqueId(), aiName, convId)) {
            Map<String, String> convMap = configManager.getSelectedConversations().get(player.getUniqueId());
            if (convMap != null && convName.equals(convMap.get(aiName))) {
                convMap.remove(aiName);
            }
            player.sendMessage(ChatColor.GREEN + configManager.getMessage("conversation-deleted",
                    Map.of("name", convName, "ai-name", aiName)));
        } else {
            player.sendMessage(ChatColor.RED + configManager.getMessage("conversation-not-found",
                    Map.of("name", convName)));
        }
        return true;
    }

    private boolean handleConversationList(Player player, String aiName) {
        if (!player.hasPermission("ollamachat.conversation.list")) {
            player.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        Map<String, String> conversations = plugin.getChatHistoryManager().listConversations(player.getUniqueId(), aiName);

        if (conversations.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + configManager.getMessage("conversation-list-empty",
                    Map.of("ai-name", aiName)));
        } else {
            String convList = String.join(", ", conversations.values());
            player.sendMessage(ChatColor.GREEN + configManager.getMessage("conversation-list",
                    Map.of("conversations", convList, "ai-name", aiName)));
        }

        String selectedConv = configManager.getSelectedConversations()
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .get(aiName);

        if (selectedConv != null) {
            if (conversations.containsValue(selectedConv)) {
                player.sendMessage(ChatColor.GREEN + configManager.getMessage("conversation-default",
                        Map.of("name", selectedConv, "ai-name", aiName)));
            } else {
                player.sendMessage(ChatColor.RED + configManager.getMessage("conversation-default-invalid",
                        Map.of("name", selectedConv, "ai-name", aiName)));
            }
        }
        return true;
    }

    private boolean handleSuggests(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("player-only", null));
            return true;
        }
        if (!sender.hasPermission("ollamachat.suggests.toggle")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("usage-ollamachat", null));
            return true;
        }

        Player player = (Player) sender;
        String subCommand = args[1].toLowerCase();

        if (subCommand.equals("on")) {
            plugin.getSuggestedResponseHandler().toggleSuggestionsForPlayer(player, true);
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("suggests-enabled",
                    Map.of("player", player.getName())));
        } else if (subCommand.equals("off")) {
            plugin.getSuggestedResponseHandler().toggleSuggestionsForPlayer(player, false);
            sender.sendMessage(ChatColor.RED + configManager.getMessage("suggests-disabled",
                    Map.of("player", player.getName())));
        } else {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("usage-ollamachat", null));
        }
        return true;
    }

    private boolean handleSuggestsPresets(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.suggests-presets.toggle")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("usage-ollamachat", null));
            return true;
        }

        String subCommand = args[1].toLowerCase();
        if (subCommand.equals("on")) {
            configManager.setSuggestedResponsePresetsEnabled(true);
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("suggests-presets-enabled", null));
        } else if (subCommand.equals("off")) {
            configManager.setSuggestedResponsePresetsEnabled(false);
            sender.sendMessage(ChatColor.RED + configManager.getMessage("suggests-presets-disabled", null));
        } else {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("usage-ollamachat", null));
        }
        return true;
    }

    private boolean handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("search-usage", null));
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "toggle":
                return handleSearchToggle(sender);
            case "status":
                return handleSearchStatus(sender);
            case "engine":
                return handleSearchEngine(sender, args);
            case "query":
                return handleSearchQuery(sender, args);
            case "setkey":
                return handleSearchSetKey(sender, args);
            case "setcount":
                return handleSearchSetCount(sender, args);
            case "addkeyword":
                return handleSearchAddKeyword(sender, args);
            case "removekeyword":
                return handleSearchRemoveKeyword(sender, args);
            case "listkeywords":
                return handleSearchListKeywords(sender);
            default:
                sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("search-usage", null));
                return true;
        }
    }

    private boolean handleSearchEngine(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.search.engine")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (args.length < 3) {
            ConfigManager.SearchEngine current = configManager.getWebSearchEngine();
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-engine-current",
                    Map.of("engine", current.getConfigName())));
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-engine-available", null));
            return true;
        }

        String engineName = args[2].toLowerCase();
        try {
            ConfigManager.SearchEngine newEngine = ConfigManager.SearchEngine.fromString(engineName);
            configManager.setWebSearchEngine(newEngine);
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-engine-changed",
                    Map.of("engine", newEngine.getConfigName())));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-engine-available", null));
        }
        return true;
    }

    private boolean handleSearchSetKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.search.setkey")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-setkey-usage-multi", null));
            return true;
        }

        String engine = args[2].toLowerCase();
        String apiKey = args[3];

        switch (engine) {
            case "bocha":
                configManager.setBochaApiKey(apiKey);
                sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-setkey-bocha", null));
                break;
            case "brave":
                configManager.setBraveApiKey(apiKey);
                sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-setkey-brave", null));
                break;
            default:
                sender.sendMessage(ChatColor.RED + configManager.getMessage("search-engine-available", null));
        }
        return true;
    }

    private boolean handleSearchToggle(CommandSender sender) {
        if (!sender.hasPermission("ollamachat.search.toggle")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        boolean newState = !configManager.isWebSearchEnabled();
        configManager.setWebSearchEnabled(newState);
        sender.sendMessage(ChatColor.GREEN + configManager.getMessage(
                newState ? "search-enabled" : "search-disabled", null));
        return true;
    }

    private boolean handleSearchStatus(CommandSender sender) {
        if (!sender.hasPermission("ollamachat.search.status")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("status", configManager.isWebSearchEnabled() ? "§aEnabled" : "§cDisabled");
        placeholders.put("api-key", configManager.getBochaApiKey() != null && !configManager.getBochaApiKey().isEmpty() ? "§aConfigured" : "§cNot Configured");
        placeholders.put("result-count", String.valueOf(configManager.getWebSearchResultCount()));
        placeholders.put("auto-trigger", configManager.isWebSearchAutoTrigger() ? "§aYes" : "§cNo");

        sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-status", placeholders));

        //
        List<String> keywords = configManager.getWebSearchTriggerKeywords();
        if (!keywords.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-keywords-list",
                    Map.of("keywords", String.join("§7, §e", keywords))));
        }

        return true;
    }

    private boolean handleSearchQuery(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.search.query")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-query-usage", null));
            return true;
        }

        String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (!configManager.isWebSearchEnabled()) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-disabled-error", null));
            return true;
        }

        if (configManager.getBochaApiKey() == null || configManager.getBochaApiKey().isEmpty()) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-no-api-key", null));
            return true;
        }

        //
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                sender.sendMessage(configManager.getMessage("websearch-starting",
                        Map.of("query", query)));

                WebSearchService webSearchService = plugin.getWebSearchService();
                List<WebSearchService.SearchResult> results =
                        webSearchService.search(query, configManager.getWebSearchResultCount()).join();

                if (results.isEmpty()) {
                    sender.sendMessage(configManager.getMessage("websearch-no-results",
                            Map.of("query", query)));
                    return;
                }

                sender.sendMessage(configManager.getMessage("websearch-completed",
                        Map.of("count", String.valueOf(results.size()))));

                //
                for (int i = 0; i < results.size(); i++) {
                    WebSearchService.SearchResult result = results.get(i);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("index", String.valueOf(i + 1));
                    placeholders.put("title", result.getTitle());
                    placeholders.put("site", result.getSiteName());
                    placeholders.put("snippet", result.getSnippet());
                    placeholders.put("url", result.getUrl());

                    sender.sendMessage(configManager.getMessage("search-result-format", placeholders));
                }

            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + configManager.getMessage("websearch-error",
                        Map.of("error", e.getMessage())));
                plugin.getLogger().severe("Search error: " + e.getMessage());
            }
        });

        return true;
    }

    private boolean handleSearchSetCount(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.search.setcount")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-setcount-usage", null));
            return true;
        }

        try {
            int count = Integer.parseInt(args[2]);
            if (count < 1 || count > 50) {
                sender.sendMessage(ChatColor.RED + configManager.getMessage("search-count-invalid", null));
                return true;
            }
            configManager.setWebSearchResultCount(count);
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-count-set",
                    Map.of("count", String.valueOf(count))));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-count-invalid", null));
        }
        return true;
    }

    private boolean handleSearchAddKeyword(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.search.keywords")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-addkeyword-usage", null));
            return true;
        }

        String keyword = args[2].toLowerCase();
        List<String> keywords = configManager.getWebSearchTriggerKeywords();

        if (keywords.contains(keyword)) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-keyword-exists",
                    Map.of("keyword", keyword)));
            return true;
        }

        keywords.add(keyword);
        configManager.setWebSearchTriggerKeywords(keywords);
        sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-keyword-added",
                Map.of("keyword", keyword)));
        return true;
    }

    private boolean handleSearchRemoveKeyword(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ollamachat.search.keywords")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-removekeyword-usage", null));
            return true;
        }

        String keyword = args[2].toLowerCase();
        List<String> keywords = configManager.getWebSearchTriggerKeywords();

        if (!keywords.contains(keyword)) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("search-keyword-not-found",
                    Map.of("keyword", keyword)));
            return true;
        }

        keywords.remove(keyword);
        configManager.setWebSearchTriggerKeywords(keywords);
        sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-keyword-removed",
                Map.of("keyword", keyword)));
        return true;
    }

    private boolean handleSearchListKeywords(CommandSender sender) {
        if (!sender.hasPermission("ollamachat.search.keywords")) {
            sender.sendMessage(ChatColor.RED + configManager.getMessage("no-permission", null));
            return true;
        }

        List<String> keywords = configManager.getWebSearchTriggerKeywords();
        if (keywords.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + configManager.getMessage("search-keywords-empty", null));
        } else {
            sender.sendMessage(ChatColor.GREEN + configManager.getMessage("search-keywords-header", null));
            for (String keyword : keywords) {
                sender.sendMessage(ChatColor.GREEN + "  §e- " + keyword);
            }
        }
        return true;
    }
}