package com.ollamachat.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for the OllamaChat plugin, allowing other plugins to interact with AI chat functionality.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * OllamaChatAPI api = (OllamaChatAPI) Bukkit.getPluginManager().getPlugin("OllamaChat");
 * if (api != null) {
 *     // Send an AI query
 *     api.sendAIQuery(player, "ollama", "What is Minecraft?")
 *         .thenAccept(response -> player.sendMessage("AI says: " + response));
 * }
 * }
 * </pre>
 */
public interface OllamaChatAPI {

    // ============================================================
    // AI Query Methods
    // ============================================================

    /**
     * Sends an AI query to the specified AI model and returns the response asynchronously.
     *
     * @param player The player initiating the query (can be null for console)
     * @param aiName The name of the AI model (e.g., "ollama" or other configured AIs)
     * @param prompt The prompt to send to the AI
     * @return A CompletableFuture containing the AI's response as a String
     */
    CompletableFuture<String> sendAIQuery(Player player, String aiName, String prompt);

    /**
     * Sends an AI query with custom conversation context.
     *
     * @param player         The player initiating the query (can be null for console)
     * @param aiName         The name of the AI model
     * @param prompt         The prompt to send to the AI
     * @param conversationId The specific conversation ID to use (null for default)
     * @return A CompletableFuture containing the AI's response as a String
     */
    CompletableFuture<String> sendAIQueryWithContext(Player player, String aiName, String prompt, String conversationId);

    /**
     * Sends an AI query with streaming response (real-time).
     *
     * @param player         The player initiating the query
     * @param aiName         The name of the AI model
     * @param prompt         The prompt to send to the AI
     * @param callback       Callback for each chunk of the streaming response
     * @return A CompletableFuture that completes when streaming finishes
     */
    CompletableFuture<Void> sendAIQueryStreaming(Player player, String aiName, String prompt, StreamCallback callback);

    /**
     * Checks if an AI model is enabled.
     *
     * @param aiName The name of the AI model
     * @return True if the AI model is enabled, false otherwise
     */
    boolean isAIEnabled(String aiName);

    /**
     * Gets the list of all available AI models.
     *
     * @return A list of available AI model names
     */
    List<String> getAvailableAIModels();

    // ============================================================
    // Conversation Management Methods
    // ============================================================

    /**
     * Creates a new conversation for a player with the specified AI model.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convName   The name of the conversation
     * @return The conversation ID, or null if creation failed
     */
    String createConversation(UUID playerUuid, String aiModel, String convName);

    /**
     * Creates a new conversation and returns the ID, with auto-select option.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convName   The name of the conversation
     * @param select     Whether to automatically select this conversation
     * @return The conversation ID, or null if creation failed
     */
    String createConversation(UUID playerUuid, String aiModel, String convName, boolean select);

    /**
     * Checks if a conversation exists by name for a player and AI model.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convName   The conversation name
     * @return True if the conversation exists, false otherwise
     */
    boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName);

    /**
     * Gets the conversation ID for a player's conversation with a specific AI model.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convName   The conversation name
     * @return The conversation ID, or null if not found
     */
    String getConversationId(UUID playerUuid, String aiModel, String convName);

    /**
     * Gets the current selected conversation for a player and AI model.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @return The conversation name, or null if none selected
     */
    String getSelectedConversation(UUID playerUuid, String aiModel);

    /**
     * Selects a conversation as the default for a player and AI model.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convName   The conversation name to select
     * @return True if selection was successful, false otherwise
     */
    boolean selectConversation(UUID playerUuid, String aiModel, String convName);

    /**
     * Deletes a conversation for a player.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convId     The conversation ID
     * @return True if the conversation was deleted, false otherwise
     */
    boolean deleteConversation(UUID playerUuid, String aiModel, String convId);

    /**
     * Deletes a conversation by name.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convName   The conversation name
     * @return True if the conversation was deleted, false otherwise
     */
    boolean deleteConversationByName(UUID playerUuid, String aiModel, String convName);

    /**
     * Lists all conversations for a player and AI model.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @return A map of conversation IDs to conversation names
     */
    Map<String, String> listConversations(UUID playerUuid, String aiModel);

    /**
     * Gets detailed information about a conversation.
     *
     * @param playerUuid The UUID of the player
     * @param aiModel    The AI model name
     * @param convId     The conversation ID
     * @return Conversation information, or null if not found
     */
    ConversationInfo getConversationInfo(UUID playerUuid, String aiModel, String convId);

    // ============================================================
    // Chat History Methods
    // ============================================================

    /**
     * Saves a chat history entry for a player.
     *
     * @param playerUuid     The UUID of the player
     * @param aiModel        The AI model name
     * @param conversationId The conversation ID (can be null for default conversation)
     * @param prompt         The user's prompt
     * @param response       The AI's response
     */
    void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response);

    /**
     * Retrieves the chat history for a player's conversation.
     *
     * @param playerUuid     The UUID of the player
     * @param aiModel        The AI model name
     * @param conversationId The conversation ID (can be null for default conversation)
     * @return The chat history as a String
     */
    String getChatHistory(UUID playerUuid, String aiModel, String conversationId);

    /**
     * Retrieves the chat history as a list of messages.
     *
     * @param playerUuid     The UUID of the player
     * @param aiModel        The AI model name
     * @param conversationId The conversation ID (can be null for default conversation)
     * @return A list of chat messages
     */
    List<ChatMessage> getChatHistoryAsList(UUID playerUuid, String aiModel, String conversationId);

    /**
     * Clears chat history for a conversation.
     *
     * @param playerUuid     The UUID of the player
     * @param aiModel        The AI model name
     * @param conversationId The conversation ID
     * @return True if cleared successfully, false otherwise
     */
    boolean clearChatHistory(UUID playerUuid, String aiModel, String conversationId);

    /**
     * Gets the maximum number of chat history entries stored.
     *
     * @return The maximum history limit
     */
    int getMaxHistory();

    /**
     * Sets the maximum number of chat history entries.
     *
     * @param maxHistory The new maximum history limit
     */
    void setMaxHistory(int maxHistory);

    // ============================================================
    // Prompt Management Methods
    // ============================================================

    /**
     * Gets all custom prompts.
     *
     * @return A map of prompt names to prompt content
     */
    Map<String, String> getPrompts();

    /**
     * Gets a specific prompt by name.
     *
     * @param name The prompt name
     * @return The prompt content, or null if not found
     */
    String getPrompt(String name);

    /**
     * Sets a custom prompt.
     *
     * @param name    The prompt name
     * @param content The prompt content
     */
    void setPrompt(String name, String content);

    /**
     * Deletes a custom prompt.
     *
     * @param name The prompt name
     * @return True if deleted, false otherwise
     */
    boolean deletePrompt(String name);

    /**
     * Gets the current default prompt name.
     *
     * @return The default prompt name, or empty string if none
     */
    String getDefaultPrompt();

    /**
     * Sets the default prompt.
     *
     * @param name The prompt name to set as default
     */
    void setDefaultPrompt(String name);

    // ============================================================
    // Web Search Methods
    // ============================================================

    /**
     * Performs a web search and returns the results.
     *
     * @param query The search query
     * @param count Maximum number of results (1-50)
     * @return A CompletableFuture containing the search results
     */
    CompletableFuture<List<SearchResult>> webSearch(String query, int count);

    /**
     * Checks if web search is enabled.
     *
     * @return True if web search is enabled, false otherwise
     */
    boolean isWebSearchEnabled();

    /**
     * Gets the current search engine.
     *
     * @return The current search engine name (bocha or brave)
     */
    String getCurrentSearchEngine();

    /**
     * Changes the search engine.
     *
     * @param engine The engine name (bocha or brave)
     * @return True if changed successfully, false otherwise
     */
    boolean setSearchEngine(String engine);

    // ============================================================
    // Utility Methods
    // ============================================================

    /**
     * Gets the plugin version.
     *
     * @return The plugin version string
     */
    String getPluginVersion();

    /**
     * Checks if the plugin is properly initialized.
     *
     * @return True if initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Gets a localized message.
     *
     * @param key The message key
     * @return The localized message
     */
    String getMessage(String key);

    /**
     * Gets a localized message with placeholders.
     *
     * @param key          The message key
     * @param placeholders Placeholder values
     * @return The localized message with placeholders replaced
     */
    String getMessage(String key, Map<String, String> placeholders);

    // ============================================================
    // Inner Classes
    // ============================================================

    /**
     * Callback interface for streaming responses.
     */
    @FunctionalInterface
    interface StreamCallback {
        /**
         * Called when a chunk of the streaming response is received.
         *
         * @param chunk The response chunk
         * @param isFirst True if this is the first chunk
         */
        void onChunk(String chunk, boolean isFirst);
    }

    /**
     * Represents a chat message in history.
     */
    class ChatMessage {
        private final String role;
        private final String content;
        private final long timestamp;

        public ChatMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Represents conversation information.
     */
    class ConversationInfo {
        private final String id;
        private final String name;
        private final String aiModel;
        private final long createdAt;
        private final int messageCount;

        public ConversationInfo(String id, String name, String aiModel, long createdAt, int messageCount) {
            this.id = id;
            this.name = name;
            this.aiModel = aiModel;
            this.createdAt = createdAt;
            this.messageCount = messageCount;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getAiModel() { return aiModel; }
        public long getCreatedAt() { return createdAt; }
        public int getMessageCount() { return messageCount; }
    }

    /**
     * Represents a web search result.
     */
    class SearchResult {
        private final String title;
        private final String url;
        private final String snippet;
        private final String siteName;

        public SearchResult(String title, String url, String snippet, String siteName) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
            this.siteName = siteName;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getSnippet() { return snippet; }
        public String getSiteName() { return siteName; }
    }
}