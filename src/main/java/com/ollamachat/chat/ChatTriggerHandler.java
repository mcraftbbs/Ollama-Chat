package com.ollamachat.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ollamachat.AIService;
import com.ollamachat.search.WebSearchService;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;
import com.ollamachat.format.MarkdownFormatter;
import com.ollamachat.format.MessageFormatter;
import com.ollamachat.scheduler.SchedulerUtils;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatTriggerHandler implements Listener {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final AIService aiService;
    private final SuggestedResponseHandler suggestedResponseHandler;
    private final WebSearchService webSearchService;
    private final Gson gson;
    private final MarkdownFormatter markdownFormatter;
    private final MessageFormatter messageFormatter;

    public ChatTriggerHandler(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.aiService = new AIService();
        this.suggestedResponseHandler = new SuggestedResponseHandler(plugin);
        this.webSearchService = plugin.getWebSearchService();
        this.gson = new Gson();
        this.markdownFormatter = configManager.getMarkdownFormatter();
        this.messageFormatter = configManager.getMessageFormatter();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!configManager.isOllamaEnabled()) return;

        String message = event.getMessage();
        Player player = event.getPlayer();

        for (String prefix : configManager.getTriggerPrefixes()) {
            if (message.startsWith(prefix)) {
                event.setCancelled(true);
                String prompt = message.substring(prefix.length()).trim();
                if (!prompt.isEmpty()) {
                    processAIQuery(player, "ollama", prompt);
                }
                break;
            }
        }
    }

    public void processAIQuery(Player player, String aiName, String prompt) {
        if (!aiName.equalsIgnoreCase("ollama") && !configManager.getOtherAIEnabled().getOrDefault(aiName, false)) {
            sendErrorMessage(player, configManager.getMessage("error-prefix", null) +
                    configManager.getMessage("toggle-disabled", Map.of("ai-name", aiName)));
            return;
        }

        // Show progress bar
        if (plugin.getConfig().getBoolean("progress-display.enabled", true)) {
            BarColor color = BarColor.valueOf(plugin.getConfig().getString("progress-display.color", "BLUE"));
            BarStyle style = BarStyle.valueOf(plugin.getConfig().getString("progress-display.style", "SOLID"));
            plugin.getProgressManager().startProgress(player,
                    configManager.getMessage("generating-status", null), color, style);
        }

        // Use SchedulerUtils for Folia compatibility
        SchedulerUtils.runAsync(plugin, () -> {
            try {
                UUID playerUuid = player.getUniqueId();
                // Save player info to ensure uuid exists in players table
                plugin.getChatHistoryManager().savePlayerInfo(player);

                String conversationName = configManager.getSelectedConversations()
                        .computeIfAbsent(playerUuid, k -> new HashMap<>())
                        .getOrDefault(aiName, null);
                String conversationId = conversationName != null
                        ? plugin.getChatHistoryManager().getConversationId(playerUuid, aiName, conversationName)
                        : null;

                // Check if web search should be performed
                boolean shouldSearch = shouldPerformWebSearch(prompt);
                String searchContext = "";

                if (shouldSearch && configManager.isWebSearchEnabled()) {
                    player.sendMessage(configManager.getMessage("websearch-starting",
                            Map.of("query", prompt)));

                    List<WebSearchService.SearchResult> searchResults =
                            webSearchService.search(prompt, configManager.getWebSearchResultCount()).join();

                    if (!searchResults.isEmpty()) {
                        searchContext = webSearchService.formatSearchResultsForAI(searchResults, prompt);
                        player.sendMessage(configManager.getMessage("websearch-completed",
                                Map.of("count", String.valueOf(searchResults.size()))));
                    } else {
                        player.sendMessage(configManager.getMessage("websearch-no-results-short", null));
                    }
                }

                // Build conversation context
                String history = plugin.getChatHistoryManager().getChatHistory(playerUuid, aiName,
                        conversationId, configManager.getMaxHistory());
                String selectedPrompt = configManager.getPrompts().getOrDefault(configManager.getDefaultPrompt(), "");

                // Build context with or without search results
                String context;
                if (!searchContext.isEmpty()) {
                    String template = configManager.getWebSearchPromptTemplate();
                    context = template
                            .replace("{search_results}", searchContext)
                            .replace("{prompt}", prompt);
                } else {
                    context = history + (selectedPrompt.isEmpty() ? "" : selectedPrompt + "\n") + "User: " + prompt;
                }

                // Get API configuration
                String apiUrl, apiKey, model;
                boolean isMessagesFormat;
                if (aiName.equalsIgnoreCase("ollama")) {
                    apiUrl = configManager.getOllamaApiUrl();
                    apiKey = null;
                    model = configManager.getOllamaModel();
                    isMessagesFormat = false;
                } else {
                    ConfigManager.AIConfig aiConfig = configManager.getOtherAIConfigs().get(aiName);
                    apiUrl = aiConfig.getApiUrl();
                    apiKey = aiConfig.getApiKey();
                    model = aiConfig.getModel();
                    isMessagesFormat = aiConfig.isMessagesFormat();
                }

                // Send request and get response
                String finalResponse;
                if (configManager.isStreamingEnabled()) {
                    StringBuilder fullResponse = new StringBuilder();
                    AtomicBoolean isFirstMessage = new AtomicBoolean(true);
                    aiService.sendStreamingRequest(apiUrl, apiKey, model, context, partialResponse -> {
                        if (player.isOnline()) {
                            // Format partial response with Markdown
                            String formattedPartial = markdownFormatter.format(partialResponse);
                            if (formattedPartial.length() > configManager.getMaxResponseLength()) {
                                formattedPartial = formattedPartial.substring(0, configManager.getMaxResponseLength()) + "...";
                            }
                            String message = isFirstMessage.get()
                                    ? configManager.getMessage("response-prefix", null) + formattedPartial
                                    : formattedPartial;
                            player.sendMessage(message);
                            isFirstMessage.set(false);
                            fullResponse.append(partialResponse);
                        }
                    }, isMessagesFormat).join();
                    finalResponse = fullResponse.toString();
                } else {
                    String responseBody = aiService.sendRequest(apiUrl, apiKey, model, context, isMessagesFormat).join();
                    if (isMessagesFormat) {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        finalResponse = json.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .get("message").getAsJsonObject()
                                .get("content").getAsString();
                    } else {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        finalResponse = json.get("response").getAsString();
                    }
                    if (player.isOnline()) {
                        sendFormattedResponse(player, finalResponse);
                    }
                }

                // Save history and send suggestions
                if (!finalResponse.isEmpty()) {
                    plugin.getChatHistoryManager().saveChatHistory(playerUuid, aiName, conversationId, prompt, finalResponse);
                    suggestedResponseHandler.sendSuggestedResponses(player, aiName, prompt, finalResponse);
                }

                // Complete progress
                SchedulerUtils.runOnEntity(plugin, player, () -> {
                    plugin.getProgressManager().complete(player);
                });

            } catch (Exception e) {
                plugin.getLogger().severe("Error processing " + aiName + " request: " + e.getMessage());
                if (player.isOnline()) {
                    sendErrorMessage(player, configManager.getMessage("error-prefix", null) +
                            configManager.getMessage("websearch-error", Map.of("error", e.getMessage())));
                }
                SchedulerUtils.runOnEntity(plugin, player, () -> {
                    plugin.getProgressManager().error(player);
                });
            }
        });
    }

    private boolean shouldPerformWebSearch(String prompt) {
        if (!configManager.isWebSearchEnabled() || !configManager.isWebSearchAutoTrigger()) {
            return false;
        }

        String lowerPrompt = prompt.toLowerCase();
        for (String keyword : configManager.getWebSearchTriggerKeywords()) {
            if (lowerPrompt.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void sendFormattedResponse(Player player, String response) {
        // Apply Markdown formatting
        String formattedResponse = markdownFormatter.format(response);

        // Truncate if too long
        if (formattedResponse.length() > configManager.getMaxResponseLength()) {
            formattedResponse = formattedResponse.substring(0, configManager.getMaxResponseLength()) + "...";
        }

        // Send with response prefix
        String prefix = configManager.getMessage("response-prefix", null);

        // Split long messages into multiple lines for better readability
        String[] lines = formattedResponse.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                player.sendMessage(prefix + line);
            }
        }
    }

    private void sendErrorMessage(Player player, String errorMessage) {
        // Apply message formatting
        String formatted = messageFormatter.error(errorMessage);
        player.sendMessage(formatted);
    }
}