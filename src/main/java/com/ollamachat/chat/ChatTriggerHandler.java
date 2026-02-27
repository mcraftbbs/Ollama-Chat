package com.ollamachat.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ollamachat.AIService;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;

public class ChatTriggerHandler implements Listener {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final AIService aiService;
    private final SuggestedResponseHandler suggestedResponseHandler;
    private WebSearchHandler webSearchHandler;
    private final Gson gson;

    public ChatTriggerHandler(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.aiService = new AIService();
        this.suggestedResponseHandler = new SuggestedResponseHandler(plugin);
        this.webSearchHandler = null;
        this.gson = new Gson();
    }

    /**
     * Sets the WebSearchHandler after initialization to avoid circular dependency
     */
    public void setWebSearchHandler(WebSearchHandler webSearchHandler) {
        this.webSearchHandler = webSearchHandler;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        if (webSearchHandler != null && webSearchHandler.isWebSearchTrigger(message)) {
            event.setCancelled(true);
            webSearchHandler.processWebSearchMessage(player, message);
            return;
        }

        if (!configManager.isOllamaEnabled()) return;

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
            sendErrorMessage(player, configManager.getMessage("error-prefix", null) + configManager.getMessage("toggle-disabled", Map.of("ai-name", aiName)));
            return;
        }

        if (plugin.getConfig().getBoolean("progress-display.enabled", true)) {
            BarColor color = BarColor.valueOf(plugin.getConfig().getString("progress-display.color", "BLUE"));
            BarStyle style = BarStyle.valueOf(plugin.getConfig().getString("progress-display.style", "SOLID"));
            plugin.getProgressManager().startProgress(player, configManager.getMessage("generating-status", null), color, style);
        }

        CompletableFuture.runAsync(() -> {
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
                String history = plugin.getChatHistoryManager().getChatHistory(playerUuid, aiName, conversationId, configManager.getMaxHistory());
                String selectedPrompt = configManager.getPrompts().getOrDefault(configManager.getDefaultPrompt(), "");
                String context = history + (selectedPrompt.isEmpty() ? "" : selectedPrompt + "\n") + "User: " + prompt;

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

                String finalResponse;
                if (configManager.isStreamingEnabled()) {
                    StringBuilder fullResponse = new StringBuilder();
                    AtomicBoolean isFirstMessage = new AtomicBoolean(true);
                    aiService.sendStreamingRequest(apiUrl, apiKey, model, context, partialResponse -> {
                        if (player.isOnline()) {
                            String formattedPartial = partialResponse.length() > configManager.getMaxResponseLength()
                                    ? partialResponse.substring(0, configManager.getMaxResponseLength()) + "..."
                                    : partialResponse;
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

                if (!finalResponse.isEmpty()) {
                    plugin.getChatHistoryManager().saveChatHistory(playerUuid, aiName, conversationId, prompt, finalResponse);
                    suggestedResponseHandler.sendSuggestedResponses(player, aiName, prompt, finalResponse);
                }
                plugin.getProgressManager().complete(player);
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                plugin.getLogger().severe("Error processing " + aiName + " request: " + errorMsg);
                if (player.isOnline()) {
                    sendErrorMessage(player, configManager.getMessage("error-prefix", null) + "Failed to get response from " + aiName);
                }
            }
        });
    }

    private void sendFormattedResponse(Player player, String response) {
        if (response.length() > configManager.getMaxResponseLength()) {
            response = response.substring(0, configManager.getMaxResponseLength()) + "...";
        }
        player.sendMessage(configManager.getMessage("response-prefix", null) + response);
    }

    private void sendErrorMessage(Player player, String errorMessage) {
        player.sendMessage(errorMessage);
    }
}



