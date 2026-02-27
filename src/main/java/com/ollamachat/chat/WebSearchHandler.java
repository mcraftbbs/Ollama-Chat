package com.ollamachat.chat;

import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import com.ollamachat.BraveSearchService;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;

/**
 * Handler for web search functionality using Brave Search API
 * Integrates search capabilities into AI chat interactions
 */
public class WebSearchHandler {

    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final ChatTriggerHandler chatTriggerHandler;
    private BraveSearchService braveSearchService;
    private static final String SEARCH_PREFIX = "/search";
    private static final String WEB_SEARCH_PREFIX = "@web";

    public WebSearchHandler(Ollamachat plugin, ChatTriggerHandler chatTriggerHandler) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatTriggerHandler = chatTriggerHandler;
        initializeSearchService();
    }

    /**
     * Initializes the Brave Search Service with the API key from config
     */
    private void initializeSearchService() {
        String apiKey = configManager.getBraveSearchApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            this.braveSearchService = new BraveSearchService(apiKey);
        } else {
            this.braveSearchService = null;
        }
    }

    /**
     * Checks if web search is enabled
     * @return true if Brave Search API key is configured
     */
    public boolean isWebSearchEnabled() {
        return braveSearchService != null && configManager.isBraveSearchEnabled();
    }

    /**
     * Checks if a message triggers web search
     * @param message The chat message
     * @return true if message contains web search trigger
     */
    public boolean isWebSearchTrigger(String message) {
        String trimmed = message.trim();
        return hasPrefix(trimmed, WEB_SEARCH_PREFIX) || hasPrefix(trimmed, SEARCH_PREFIX);
    }

    /**
     * Processes a message that triggers web search
     * Combines search results with LLM for intelligent answering
     * @param player The player
     * @param message The message containing search trigger
     */
    public void processWebSearchMessage(Player player, String message) {
        String trimmed = message.trim();
        if (hasPrefix(trimmed, WEB_SEARCH_PREFIX)) {
            String query = trimmed.substring(WEB_SEARCH_PREFIX.length()).trim();
            if (!query.isEmpty()) {
                performSearchAndAskAI(player, query);
            } else {
                player.sendMessage(configManager.getMessage("search-empty-query", null));
            }
        } else if (hasPrefix(trimmed, SEARCH_PREFIX)) {
            String query = trimmed.substring(SEARCH_PREFIX.length()).trim();
            if (!query.isEmpty()) {
                performSearchAndAskAI(player, query);
            } else {
                player.sendMessage(configManager.getMessage("search-empty-query", null));
            }
        }
    }

    private boolean hasPrefix(String message, String prefix) {
        return message.toLowerCase().startsWith(prefix.toLowerCase());
    }

    /**
     * Performs web search and sends results to LLM for processing
     * @param player The player
     * @param query The search query
     */
    private void performSearchAndAskAI(Player player, String query) {
        if (!isWebSearchEnabled()) {
            player.sendMessage(configManager.getMessage("error-prefix", null) + "Web search is not enabled!");
            return;
        }

        player.sendMessage(configManager.getMessage("search-searching", null));

        CompletableFuture<String> searchFuture = braveSearchService.search(query);

        searchFuture
                .thenAccept(searchResults -> {
                    if (searchResults != null && !searchResults.isEmpty()) {
                        // Create enhanced prompt with search results
                        String enhancedPrompt = createEnhancedPromptWithSearchResults(query, searchResults);
                        
                        // Send the enhanced prompt to AI for processing
                        chatTriggerHandler.processAIQuery(player, "ollama", enhancedPrompt);
                    } else {
                        player.sendMessage(configManager.getMessage("error-prefix", null) + "No search results found.");
                    }
                })
                .exceptionally(e -> {
                    String errorMsg = configManager.getMessage("error-prefix", null) +
                            configManager.getMessage("search-error", null) + ": " + e.getMessage();
                    player.sendMessage(errorMsg);
                    plugin.getLogger().warning("Web search error: " + e.getMessage());
                    return null;
                });
    }

    /**
     * Extracts search context for AI prompt enhancement
     * Combines search results with original prompt for context-aware answering
     * @param originalPrompt The original user prompt
     * @param searchResults The web search results
     * @return Enhanced prompt with search context
     */
    public String createEnhancedPromptWithSearchResults(String originalPrompt, String searchResults) {
        return "You are helpful assistant. Please answer the following user question based on the provided web search results.\n\n" +
                "=== USER QUESTION ===\n" +
                originalPrompt + "\n\n" +
                "=== WEB SEARCH RESULTS ===\n" +
                searchResults + "\n\n" +
                "=== INSTRUCTIONS ===\n" +
                "1. Provide a comprehensive and natural-sounding answer based on the search results\n" +
                "2. If the search results don't contain enough information, state that clearly\n" +
                "3. Be concise and informative in your response\n" +
                "4. Just use new line or punctuation mark to separate paragraphs, don't use tables or lists";
    }

    /**
     * Reloads the search service (useful for config reload)
     */
    public void reload() {
        initializeSearchService();
    }
}

