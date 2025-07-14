package com.ollamachat.core;

import com.ollamachat.ChatHistoryManager;
import com.ollamachat.DatabaseManager;
import com.ollamachat.ProgressManager;
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

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.initialize();

        try {
            databaseManager = new DatabaseManager(this);
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

        // Register events
        getServer().getPluginManager().registerEvents(new ChatTriggerHandler(this), this);

        // Register commands
        getCommand("ollamachat").setExecutor(new OllamaChatCommand(this));
        getCommand("ollamachat").setTabCompleter(new OllamaChatTabCompleter(this));
        getCommand("aichat").setExecutor(new AIChatCommand(this));
        getCommand("aichat").setTabCompleter(new OllamaChatTabCompleter(this));
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

    // Getter methods
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





