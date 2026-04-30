package com.ollamachat.core;

import com.ollamachat.Metrics;
import com.ollamachat.ChatHistoryManager;
import com.ollamachat.DependencyLoader;
import com.ollamachat.ProgressManager;
import com.ollamachat.DatabaseManager;
import com.ollamachat.api.OllamaChatAPI;
import com.ollamachat.api.OllamaChatAPIImpl;
import com.ollamachat.chat.ChatTriggerHandler;
import com.ollamachat.chat.SuggestedResponseHandler;
import com.ollamachat.command.AIChatCommand;
import com.ollamachat.command.OllamaChatCommand;
import com.ollamachat.command.OllamaChatTabCompleter;
import com.ollamachat.search.WebSearchService;
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
    private WebSearchService webSearchService;
    private Map<UUID, Boolean> playerSuggestionToggles;
    private OllamaChatAPI api;
    private ChatTriggerHandler chatTriggerHandler;

    @Override
    public void onEnable() {
        DependencyLoader loader = new DependencyLoader(this);
        if (!loader.loadDependencies()) {
            getLogger().severe("Failed to load dependencies. ");
            return;
        }

        configManager = new ConfigManager(this);
        configManager.initialize();

        // You can find the plugin id of your plugins on
        // the page https://bstats.org/what-is-my-plugin-id
        int pluginId = 30682;
        Metrics metrics = new Metrics(this, pluginId);

        // Optional: Add custom charts
        metrics.addCustomChart(
                new Metrics.SimplePie("chart_id", () -> "My value")
        );

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
        webSearchService = new WebSearchService(this);
        playerSuggestionToggles = new HashMap<>();
        api = new OllamaChatAPIImpl(this);
        chatTriggerHandler = new ChatTriggerHandler(this);

        getServer().getPluginManager().registerEvents(chatTriggerHandler, this);

        getCommand("ollamachat").setExecutor(new OllamaChatCommand(this));
        getCommand("ollamachat").setTabCompleter(new OllamaChatTabCompleter(this));
        getCommand("aichat").setExecutor(new AIChatCommand(this));
        getCommand("aichat").setTabCompleter(new OllamaChatTabCompleter(this));

        getLogger().info("OllamaChat API is available for other plugins.");
        getLogger().info("OllamaChat v" + getDescription().getVersion() + " enabled successfully!");

        if (configManager.isWebSearchEnabled()) {
            if (configManager.getBochaApiKey() == null || configManager.getBochaApiKey().isEmpty()) {
                getLogger().warning("Web search is enabled but Bocha API key is not configured!");
            } else {
                getLogger().info("Web search feature is enabled with Bocha API");
            }
        }
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

    public void reloadPlugin() {
        configManager.reloadConfigValues();
        configManager.reloadLanguage();
        if (chatHistoryManager != null) {
        }
        getLogger().info("Plugin reloaded successfully!");
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

    public WebSearchService getWebSearchService() {
        return webSearchService;
    }

    public Map<UUID, Boolean> getPlayerSuggestionToggles() {
        return playerSuggestionToggles;
    }
}