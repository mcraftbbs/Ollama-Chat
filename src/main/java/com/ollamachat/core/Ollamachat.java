package com.ollamachat.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import com.ollamachat.ChatHistoryManager;
import com.ollamachat.DatabaseManager;
import com.ollamachat.DependencyLoader;
import com.ollamachat.ProgressManager;
import com.ollamachat.api.OllamaChatAPI;
import com.ollamachat.api.OllamaChatAPIImpl;
import com.ollamachat.chat.ChatTriggerHandler;
import com.ollamachat.chat.SuggestedResponseHandler;
import com.ollamachat.chat.WebSearchHandler;
import com.ollamachat.command.AIChatCommand;
import com.ollamachat.command.OllamaChatCommand;
import com.ollamachat.command.OllamaChatTabCompleter;

public class Ollamachat extends JavaPlugin {
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ChatHistoryManager chatHistoryManager;
    private ProgressManager progressManager;
    private SuggestedResponseHandler suggestedResponseHandler;
    private WebSearchHandler webSearchHandler;
    private Map<UUID, Boolean> playerSuggestionToggles;
    private OllamaChatAPI api;

    @Override
    public void onEnable() {
        DependencyLoader loader = new DependencyLoader(this);
        if (!loader.loadDependencies()) {
            getLogger().severe("Failed to load dependencies. ");
            return;
        }

        configManager = new ConfigManager(this);
        configManager.initialize();

        try {
            databaseManager = new DatabaseManager(this);
        } catch (Exception e) {
            getLogger().severe("Failed to initialize DatabaseManager: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int maxHistory = configManager.getMaxHistory();
        chatHistoryManager = new ChatHistoryManager(databaseManager, maxHistory);
        progressManager = new ProgressManager(this);
        suggestedResponseHandler = new SuggestedResponseHandler(this);
        playerSuggestionToggles = new HashMap<>();
        api = new OllamaChatAPIImpl(this);

        // Initialize ChatTriggerHandler first, then set WebSearchHandler to avoid circular dependency
        ChatTriggerHandler chatTriggerHandler = new ChatTriggerHandler(this);
        webSearchHandler = new WebSearchHandler(this, chatTriggerHandler);
        chatTriggerHandler.setWebSearchHandler(webSearchHandler);

        getServer().getPluginManager().registerEvents(chatTriggerHandler, this);

        getCommand("ollamachat").setExecutor(new OllamaChatCommand(this));
        getCommand("ollamachat").setTabCompleter(new OllamaChatTabCompleter(this));
        getCommand("aichat").setExecutor(new AIChatCommand(this));
        getCommand("aichat").setTabCompleter(new OllamaChatTabCompleter(this));

        getLogger().info("OllamaChat API is available for other plugins.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        } else {
            getLogger().warning("DatabaseManager was null, skipping close.");
        }
        if (progressManager != null) {
            getServer().getOnlinePlayers().forEach(progressManager::cleanup);
        }
    }

    public OllamaChatAPI getAPI() {
        return api;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ChatHistoryManager getChatHistoryManager() {
        return chatHistoryManager;
    }

    public ProgressManager getProgressManager() {
        return progressManager;
    }

    public SuggestedResponseHandler getSuggestedResponseHandler() {
        return suggestedResponseHandler;
    }

    public WebSearchHandler getWebSearchHandler() {
        return webSearchHandler;
    }

    public Map<UUID, Boolean> getPlayerSuggestionToggles() {
        return playerSuggestionToggles;
    }
}