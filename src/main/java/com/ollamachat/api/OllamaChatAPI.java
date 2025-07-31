package com.ollamachat.api;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for the OllamaChat plugin, allowing other plugins to interact with AI chat functionality.
 */
public interface OllamaChatAPI {

    /**
     * Sends an AI query to the specified AI model and returns the response asynchronously.
     *
     * @param player   The player initiating the query.
     * @param aiName   The name of the AI model (e.g., "ollama" or other configured AIs).
     * @param prompt   The prompt to send to the AI.
     * @return A CompletableFuture containing the AI's response as a String.
     */
    CompletableFuture<String> sendAIQuery(Player player, String aiName, String prompt);

    /**
     * Creates a new conversation for a player with the specified AI model.
     *
     * @param playerUuid The UUID of the player.
     * @param aiModel    The AI model name.
     * @param convName   The name of the conversation.
     * @return The conversation ID, or null if creation failed.
     */
    String createConversation(UUID playerUuid, String aiModel, String convName);

    /**
     * Checks if a conversation exists by name for a player and AI model.
     *
     * @param playerUuid The UUID of the player.
     * @param aiModel    The AI model name.
     * @param convName   The conversation name.
     * @return True if the conversation exists, false otherwise.
     */
    boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName);

    /**
     * Gets the conversation ID for a player's conversation with a specific AI model.
     *
     * @param playerUuid The UUID of the player.
     * @param aiModel    The AI model name.
     * @param convName   The conversation name.
     * @return The conversation ID, or null if not found.
     */
    String getConversationId(UUID playerUuid, String aiModel, String convName);

    /**
     * Deletes a conversation for a player.
     *
     * @param playerUuid The UUID of the player.
     * @param aiModel    The AI model name.
     * @param convId     The conversation ID.
     * @return True if the conversation was deleted, false otherwise.
     */
    boolean deleteConversation(UUID playerUuid, String aiModel, String convId);

    /**
     * Lists all conversations for a player and AI model.
     *
     * @param playerUuid The UUID of the player.
     * @param aiModel    The AI model name.
     * @return A map of conversation IDs to conversation names.
     */
    Map<String, String> listConversations(UUID playerUuid, String aiModel);

    /**
     * Saves a chat history entry for a player.
     *
     * @param playerUuid     The UUID of the player.
     * @param aiModel        The AI model name.
     * @param conversationId The conversation ID (can be null for default conversation).
     * @param prompt         The user's prompt.
     * @param response       The AI's response.
     */
    void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response);

    /**
     * Retrieves the chat history for a player's conversation.
     *
     * @param playerUuid     The UUID of the player.
     * @param aiModel        The AI model name.
     * @param conversationId The conversation ID (can be null for default conversation).
     * @return The chat history as a String.
     */
    String getChatHistory(UUID playerUuid, String aiModel, String conversationId);

    /**
     * Gets the maximum number of chat history entries stored.
     *
     * @return The maximum history limit.
     */
    int getMaxHistory();
}

