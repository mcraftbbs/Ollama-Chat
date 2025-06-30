package com.ollamachat.core;

import com.ollamachat.ChatHistoryManager;
import com.ollamachat.DatabaseManager;
import com.ollamachat.ProgressManager;
import com.ollamachat.command.AIChatCommand;
import com.ollamachat.command.OllamaChatCommand;
import com.ollamachat.command.OllamaChatTabCompleter;
import com.ollamachat.chat.ChatTriggerHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class Ollamachat extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ChatHistoryManager chatHistoryManager;
    private ProgressManager progressManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.initialize();

        try {
            databaseManager = new DatabaseManager();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize DatabaseManager: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int maxHistory = configManager.getMaxHistory();
        chatHistoryManager = new ChatHistoryManager(databaseManager, maxHistory);
        progressManager = new ProgressManager(this);

        // 注册事件
        getServer().getPluginManager().registerEvents(new ChatTriggerHandler(this), this);

        // 注册命令
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
        getServer().getOnlinePlayers().forEach(progressManager::cleanup);
    }

    // Getter 方法
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
}