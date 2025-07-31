package com.ollamachat.api;

import com.ollamachat.AIService;
import com.ollamachat.ChatHistoryManager;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OllamaChatAPIImpl implements OllamaChatAPI {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final ChatHistoryManager chatHistoryManager;
    private final AIService aiService;
    private final Gson gson;

    public OllamaChatAPIImpl(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatHistoryManager = plugin.getChatHistoryManager();
        this.aiService = new AIService();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<String> sendAIQuery(Player player, String aiName, String prompt) {
        if (!aiName.equalsIgnoreCase("ollama") && !configManager.getOtherAIEnabled().getOrDefault(aiName, false)) {
            return CompletableFuture.completedFuture("Error: AI model " + aiName + " is disabled.");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerUuid = player.getUniqueId();
                chatHistoryManager.savePlayerInfo(player);

                String conversationName = configManager.getSelectedConversations()
                        .computeIfAbsent(playerUuid, k -> new HashMap<>())
                        .getOrDefault(aiName, null);
                String conversationId = conversationName != null
                        ? chatHistoryManager.getConversationId(playerUuid, aiName, conversationName)
                        : null;
                String history = chatHistoryManager.getChatHistory(playerUuid, aiName, conversationId, configManager.getMaxHistory());
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

                String responseBody = aiService.sendRequest(apiUrl, apiKey, model, context, isMessagesFormat).join();
                String finalResponse;
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

                if (!finalResponse.isEmpty()) {
                    chatHistoryManager.saveChatHistory(playerUuid, aiName, conversationId, prompt, finalResponse);
                }
                return finalResponse.length() > configManager.getMaxResponseLength()
                        ? finalResponse.substring(0, configManager.getMaxResponseLength()) + "..."
                        : finalResponse;
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing API " + aiName + " request: " + e.getMessage());
                return "Error: Failed to get response from " + aiName;
            }
        });
    }

    @Override
    public String createConversation(UUID playerUuid, String aiModel, String convName) {
        return chatHistoryManager.createConversation(playerUuid, aiModel, convName);
    }

    @Override
    public boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName) {
        return chatHistoryManager.conversationExistsByName(playerUuid, aiModel, convName);
    }

    @Override
    public String getConversationId(UUID playerUuid, String aiModel, String convName) {
        return chatHistoryManager.getConversationId(playerUuid, aiModel, convName);
    }

    @Override
    public boolean deleteConversation(UUID playerUuid, String aiModel, String convId) {
        return chatHistoryManager.deleteConversation(playerUuid, aiModel, convId);
    }

    @Override
    public Map<String, String> listConversations(UUID playerUuid, String aiModel) {
        return chatHistoryManager.listConversations(playerUuid, aiModel);
    }

    @Override
    public void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response) {
        chatHistoryManager.saveChatHistory(playerUuid, aiModel, conversationId, prompt, response);
    }

    @Override
    public String getChatHistory(UUID playerUuid, String aiModel, String conversationId) {
        return chatHistoryManager.getChatHistory(playerUuid, aiModel, conversationId, configManager.getMaxHistory());
    }

    @Override
    public int getMaxHistory() {
        return configManager.getMaxHistory();
    }
}

