package com.ollamachat;

import org.bukkit.entity.Player;

import java.util.UUID;

public class ChatHistoryManager {
    private final DatabaseManager databaseManager;
    private final int maxHistory;

    public ChatHistoryManager(DatabaseManager databaseManager, int maxHistory) {
        this.databaseManager = databaseManager;
        this.maxHistory = maxHistory;
    }

    public void savePlayerInfo(Player player) {
        databaseManager.savePlayerInfo(player.getUniqueId(), player.getName());
    }

    public void saveChatHistory(UUID playerUuid, String aiModel, String prompt, String response) {
        databaseManager.saveChatHistory(playerUuid, aiModel, prompt, response);
    }

    public String getChatHistory(UUID playerUuid, String aiModel) {
        return databaseManager.getChatHistory(playerUuid, aiModel, maxHistory);
    }
}
