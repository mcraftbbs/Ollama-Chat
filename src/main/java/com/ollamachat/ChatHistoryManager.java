
package com.ollamachat;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
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

    public String createConversation(UUID playerUuid, String aiModel, String convName) {
        return databaseManager.createConversation(playerUuid, aiModel, convName);
    }

    public boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName) {
        return databaseManager.conversationExistsByName(playerUuid, aiModel, convName);
    }

    public String getConversationId(UUID playerUuid, String aiModel, String convName) {
        return databaseManager.getConversationId(playerUuid, aiModel, convName);
    }

    public boolean conversationExists(UUID playerUuid, String aiModel, String convId) {
        return databaseManager.conversationExists(playerUuid, aiModel, convId);
    }

    public boolean deleteConversation(UUID playerUuid, String aiModel, String convId) {
        return databaseManager.deleteConversation(playerUuid, aiModel, convId);
    }

    public Map<String, String> listConversations(UUID playerUuid, String aiModel) {
        return databaseManager.listConversations(playerUuid, aiModel);
    }

    public void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response) {
        databaseManager.saveChatHistory(playerUuid, aiModel, conversationId, prompt, response);
    }

    public String getChatHistory(UUID playerUuid, String aiModel, String conversationId) {
        return databaseManager.getChatHistory(playerUuid, aiModel, conversationId, maxHistory);
    }
}