package com.ollamachat.api;

import com.ollamachat.AIService;
import com.ollamachat.ChatHistoryManager;
import com.ollamachat.search.WebSearchService;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class OllamaChatAPIImpl implements OllamaChatAPI {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final ChatHistoryManager chatHistoryManager;
    private final AIService aiService;
    private final WebSearchService webSearchService;
    private final Gson gson;
    private final Map<UUID, Map<String, String>> conversationCache;
    private boolean initialized;

    public OllamaChatAPIImpl(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatHistoryManager = plugin.getChatHistoryManager();
        this.aiService = new AIService();
        this.webSearchService = plugin.getWebSearchService();
        this.gson = new Gson();
        this.conversationCache = new ConcurrentHashMap<>();
        this.initialized = true;
    }

    // ============================================================
    // AI Query Methods
    // ============================================================

    @Override
    public CompletableFuture<String> sendAIQuery(Player player, String aiName, String prompt) {
        return sendAIQueryWithContext(player, aiName, prompt, null);
    }

    @Override
    public CompletableFuture<String> sendAIQueryWithContext(Player player, String aiName, String prompt, String conversationId) {
        if (!isAIEnabled(aiName)) {
            return CompletableFuture.completedFuture("Error: AI model " + aiName + " is disabled.");
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            return CompletableFuture.completedFuture("Error: Prompt cannot be empty.");
        }

        UUID playerUuid = player != null ? player.getUniqueId() : null;

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Save player info if player exists
                if (player != null) {
                    chatHistoryManager.savePlayerInfo(player);
                }

                // Get or create conversation ID
                String convId = conversationId;
                if (convId == null && playerUuid != null) {
                    String convName = getSelectedConversation(playerUuid, aiName);
                    if (convName != null) {
                        convId = chatHistoryManager.getConversationId(playerUuid, aiName, convName);
                    }
                }

                // Build context
                String history = "";
                if (playerUuid != null) {
                    history = chatHistoryManager.getChatHistory(playerUuid, aiName, convId, configManager.getMaxHistory());
                }
                String selectedPrompt = configManager.getPrompts().getOrDefault(configManager.getDefaultPrompt(), "");
                String context = history + (selectedPrompt.isEmpty() ? "" : selectedPrompt + "\n") + "User: " + prompt;

                // Get API configuration
                ApiConfig apiConfig = getApiConfig(aiName);
                if (apiConfig == null) {
                    return "Error: Failed to get API configuration for " + aiName;
                }

                // Send request
                String responseBody = aiService.sendRequest(
                        apiConfig.url, apiConfig.key, apiConfig.model, context, apiConfig.isMessagesFormat
                ).join();

                // Parse response
                String finalResponse = parseResponse(responseBody, apiConfig.isMessagesFormat);
                if (finalResponse == null || finalResponse.isEmpty()) {
                    return "Error: Empty response from AI";
                }

                // Save history
                if (playerUuid != null && finalResponse != null && !finalResponse.isEmpty()) {
                    chatHistoryManager.saveChatHistory(playerUuid, aiName, convId, prompt, finalResponse);
                }

                // Truncate if needed
                return truncateResponse(finalResponse);
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing API " + aiName + " request: " + e.getMessage());
                e.printStackTrace();
                return "Error: Failed to get response from " + aiName + " - " + e.getMessage();
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendAIQueryStreaming(Player player, String aiName, String prompt, StreamCallback callback) {
        if (!isAIEnabled(aiName)) {
            callback.onChunk("Error: AI model " + aiName + " is disabled.", true);
            return CompletableFuture.completedFuture(null);
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onChunk("Error: Prompt cannot be empty.", true);
            return CompletableFuture.completedFuture(null);
        }

        UUID playerUuid = player != null ? player.getUniqueId() : null;

        return CompletableFuture.runAsync(() -> {
            try {
                if (player != null) {
                    chatHistoryManager.savePlayerInfo(player);
                }

                String convId = null;
                if (playerUuid != null) {
                    String convName = getSelectedConversation(playerUuid, aiName);
                    if (convName != null) {
                        convId = chatHistoryManager.getConversationId(playerUuid, aiName, convName);
                    }
                }

                String history = "";
                if (playerUuid != null) {
                    history = chatHistoryManager.getChatHistory(playerUuid, aiName, convId, configManager.getMaxHistory());
                }
                String selectedPrompt = configManager.getPrompts().getOrDefault(configManager.getDefaultPrompt(), "");
                String context = history + (selectedPrompt.isEmpty() ? "" : selectedPrompt + "\n") + "User: " + prompt;

                ApiConfig apiConfig = getApiConfig(aiName);
                if (apiConfig == null) {
                    callback.onChunk("Error: Failed to get API configuration for " + aiName, true);
                    return;
                }

                StringBuilder fullResponse = new StringBuilder();
                AtomicBoolean isFirst = new AtomicBoolean(true);

                aiService.sendStreamingRequest(apiConfig.url, apiConfig.key, apiConfig.model, context,
                        chunk -> {
                            String formattedChunk = truncateResponse(chunk);
                            callback.onChunk(formattedChunk, isFirst.getAndSet(false));
                            fullResponse.append(chunk);
                        }, apiConfig.isMessagesFormat).join();

                if (playerUuid != null && fullResponse.length() > 0) {
                    chatHistoryManager.saveChatHistory(playerUuid, aiName, convId, prompt, fullResponse.toString());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in streaming API request: " + e.getMessage());
                callback.onChunk("Error: " + e.getMessage(), true);
            }
        });
    }

    @Override
    public boolean isAIEnabled(String aiName) {
        if (aiName == null) return false;
        if (aiName.equalsIgnoreCase("ollama")) {
            return configManager.isOllamaEnabled();
        }
        return configManager.getOtherAIEnabled().getOrDefault(aiName, false);
    }

    @Override
    public List<String> getAvailableAIModels() {
        List<String> models = new ArrayList<>();
        models.add("ollama");
        models.addAll(configManager.getOtherAIConfigs().keySet());
        return models;
    }

    // ============================================================
    // Conversation Management Methods
    // ============================================================

    @Override
    public String createConversation(UUID playerUuid, String aiModel, String convName) {
        return createConversation(playerUuid, aiModel, convName, false);
    }

    @Override
    public String createConversation(UUID playerUuid, String aiModel, String convName, boolean select) {
        if (playerUuid == null || aiModel == null || convName == null) {
            return null;
        }

        String convId = chatHistoryManager.createConversation(playerUuid, aiModel, convName);
        if (convId != null && select) {
            selectConversation(playerUuid, aiModel, convName);
        }
        return convId;
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
    public String getSelectedConversation(UUID playerUuid, String aiModel) {
        return configManager.getSelectedConversations()
                .computeIfAbsent(playerUuid, k -> new HashMap<>())
                .get(aiModel);
    }

    @Override
    public boolean selectConversation(UUID playerUuid, String aiModel, String convName) {
        if (!conversationExistsByName(playerUuid, aiModel, convName)) {
            return false;
        }
        configManager.getSelectedConversations()
                .computeIfAbsent(playerUuid, k -> new HashMap<>())
                .put(aiModel, convName);
        return true;
    }

    @Override
    public boolean deleteConversation(UUID playerUuid, String aiModel, String convId) {
        return chatHistoryManager.deleteConversation(playerUuid, aiModel, convId);
    }

    @Override
    public boolean deleteConversationByName(UUID playerUuid, String aiModel, String convName) {
        String convId = getConversationId(playerUuid, aiModel, convName);
        if (convId == null) return false;
        return deleteConversation(playerUuid, aiModel, convId);
    }

    @Override
    public Map<String, String> listConversations(UUID playerUuid, String aiModel) {
        return chatHistoryManager.listConversations(playerUuid, aiModel);
    }

    @Override
    public ConversationInfo getConversationInfo(UUID playerUuid, String aiModel, String convId) {
        Map<String, String> conversations = listConversations(playerUuid, aiModel);
        String convName = conversations.get(convId);
        if (convName == null) return null;

        String history = getChatHistory(playerUuid, aiModel, convId);
        int messageCount = history.split("\n\n").length;

        return new ConversationInfo(convId, convName, aiModel, System.currentTimeMillis(), messageCount);
    }

    // ============================================================
    // Chat History Methods
    // ============================================================

    @Override
    public void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response) {
        chatHistoryManager.saveChatHistory(playerUuid, aiModel, conversationId, prompt, response);
    }

    @Override
    public String getChatHistory(UUID playerUuid, String aiModel, String conversationId) {
        return chatHistoryManager.getChatHistory(playerUuid, aiModel, conversationId, configManager.getMaxHistory());
    }

    @Override
    public List<ChatMessage> getChatHistoryAsList(UUID playerUuid, String aiModel, String conversationId) {
        String history = getChatHistory(playerUuid, aiModel, conversationId);
        List<ChatMessage> messages = new ArrayList<>();

        if (history == null || history.isEmpty()) {
            return messages;
        }

        String[] lines = history.split("\n");
        for (String line : lines) {
            if (line.startsWith("User: ")) {
                messages.add(new ChatMessage("user", line.substring(6), System.currentTimeMillis()));
            } else if (line.startsWith("AI: ")) {
                messages.add(new ChatMessage("assistant", line.substring(4), System.currentTimeMillis()));
            }
        }

        return messages;
    }

    @Override
    public boolean clearChatHistory(UUID playerUuid, String aiModel, String conversationId) {
        // This would require a new method in DatabaseManager
        // For now, return false
        plugin.getLogger().warning("clearChatHistory is not yet implemented");
        return false;
    }

    @Override
    public int getMaxHistory() {
        return configManager.getMaxHistory();
    }

    @Override
    public void setMaxHistory(int maxHistory) {
        // This would require updating config and DatabaseManager
        plugin.getLogger().warning("setMaxHistory is not yet implemented");
    }

    // ============================================================
    // Prompt Management Methods
    // ============================================================

    @Override
    public Map<String, String> getPrompts() {
        return Collections.unmodifiableMap(configManager.getPrompts());
    }

    @Override
    public String getPrompt(String name) {
        return configManager.getPrompts().get(name);
    }

    @Override
    public void setPrompt(String name, String content) {
        configManager.addPrompt(name, content);
    }

    @Override
    public boolean deletePrompt(String name) {
        if (!configManager.getPrompts().containsKey(name)) {
            return false;
        }
        configManager.removePrompt(name);
        return true;
    }

    @Override
    public String getDefaultPrompt() {
        return configManager.getDefaultPrompt();
    }

    @Override
    public void setDefaultPrompt(String name) {
        configManager.setDefaultPrompt(name);
    }

    // ============================================================
    // Web Search Methods
    // ============================================================

    @Override
    public CompletableFuture<List<SearchResult>> webSearch(String query, int count) {
        if (!isWebSearchEnabled()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return webSearchService.search(query, Math.min(count, 50))
                .thenApply(results -> {
                    List<SearchResult> apiResults = new ArrayList<>();
                    for (WebSearchService.SearchResult result : results) {
                        apiResults.add(new SearchResult(
                                result.getTitle(),
                                result.getUrl(),
                                result.getSnippet(),
                                result.getSiteName()
                        ));
                    }
                    return apiResults;
                });
    }

    @Override
    public boolean isWebSearchEnabled() {
        return configManager.isWebSearchEnabled();
    }

    @Override
    public String getCurrentSearchEngine() {
        return configManager.getWebSearchEngine().getConfigName();
    }

    @Override
    public boolean setSearchEngine(String engine) {
        try {
            ConfigManager.SearchEngine newEngine = ConfigManager.SearchEngine.fromString(engine);
            configManager.setWebSearchEngine(newEngine);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ============================================================
    // Utility Methods
    // ============================================================

    @Override
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String getMessage(String key) {
        return configManager.getMessage(key, null);
    }

    @Override
    public String getMessage(String key, Map<String, String> placeholders) {
        return configManager.getMessage(key, placeholders);
    }

    // ============================================================
    // Private Helper Methods
    // ============================================================

    private ApiConfig getApiConfig(String aiName) {
        if (aiName.equalsIgnoreCase("ollama")) {
            return new ApiConfig(
                    configManager.getOllamaApiUrl(),
                    null,
                    configManager.getOllamaModel(),
                    false
            );
        } else {
            ConfigManager.AIConfig aiConfig = configManager.getOtherAIConfigs().get(aiName);
            if (aiConfig == null) return null;
            return new ApiConfig(
                    aiConfig.getApiUrl(),
                    aiConfig.getApiKey(),
                    aiConfig.getModel(),
                    aiConfig.isMessagesFormat()
            );
        }
    }

    private String parseResponse(String responseBody, boolean isMessagesFormat) {
        try {
            if (isMessagesFormat) {
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                return json.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .get("message").getAsJsonObject()
                        .get("content").getAsString();
            } else {
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                return json.get("response").getAsString();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse response: " + e.getMessage());
            return responseBody;
        }
    }

    private String truncateResponse(String response) {
        int maxLength = configManager.getMaxResponseLength();
        if (response != null && response.length() > maxLength) {
            return response.substring(0, maxLength) + "...";
        }
        return response;
    }

    private static class ApiConfig {
        final String url;
        final String key;
        final String model;
        final boolean isMessagesFormat;

        ApiConfig(String url, String key, String model, boolean isMessagesFormat) {
            this.url = url;
            this.key = key;
            this.model = model;
            this.isMessagesFormat = isMessagesFormat;
        }
    }
}