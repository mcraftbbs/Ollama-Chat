package com.ollamachat.core;

import com.ollamachat.ChatHistoryManager;
import com.ollamachat.DatabaseManager;
import com.ollamachat.DependencyLoader;
import com.ollamachat.ProgressManager;
import com.ollamachat.api.OllamaChatAPI;
import com.ollamachat.api.OllamaChatAPIImpl;
import com.ollamachat.chat.ChatTriggerHandler;
import com.ollamachat.chat.SuggestedResponseHandler;
import com.ollamachat.command.AIChatCommand;
import com.ollamachat.command.OllamaChatCommand;
import com.ollamachat.command.OllamaChatTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Ollamachat extends JavaPlugin {
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ChatHistoryManager chatHistoryManager;
    private ProgressManager progressManager;
    private SuggestedResponseHandler suggestedResponseHandler;
    private Map<UUID, Boolean> playerSuggestionToggles;
    private OllamaChatAPI api;

    @Override
    public void onEnable() {
        DependencyLoader dependencyLoader = new DependencyLoader(this);
        ClassLoader dependencyClassLoader;
        try {
            dependencyLoader.loadDependencies();
            dependencyClassLoader = dependencyLoader.getClass().getClassLoader();
        } catch (Exception e) {
            getLogger().severe("Failed to load dependencies: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        configManager.initialize();

        try {
            databaseManager = new DatabaseManager(this, dependencyClassLoader);
        } catch (Exception e) {
            getLogger().severe("Failed to initialize DatabaseManager: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int maxHistory = configManager.getMaxHistory();
        chatHistoryManager = new ChatHistoryManager(databaseManager, maxHistory);
        progressManager = new ProgressManager(this);
        suggestedResponseHandler = new SuggestedResponseHandler(this);
        playerSuggestionToggles = new HashMap<>();
        api = new OllamaChatAPIImpl(this);

        getServer().getPluginManager().registerEvents(new ChatTriggerHandler(this), this);

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

    public Map<UUID, Boolean> getPlayerSuggestionToggles() {
        return playerSuggestionToggles;
    }
}









