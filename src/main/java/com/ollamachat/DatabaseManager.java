package com.ollamachat;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private Connection connection;

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:plugins/OllamaChat/chat_history.db");
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY," +
                    "username TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS conversations (" +
                    "conversation_id TEXT NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "ai_model TEXT NOT NULL," +
                    "conversation_name TEXT NOT NULL," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (conversation_id, player_uuid, ai_model)," +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid))");

            stmt.execute("CREATE TABLE IF NOT EXISTS chat_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "ai_model TEXT NOT NULL," +
                    "conversation_id TEXT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "prompt TEXT NOT NULL," +
                    "response TEXT NOT NULL," +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid)," +
                    "FOREIGN KEY (conversation_id, player_uuid, ai_model) REFERENCES conversations(conversation_id, player_uuid, ai_model))");
        }
    }

    public void savePlayerInfo(UUID uuid, String username) {
        String sql = "INSERT OR REPLACE INTO players(uuid, username) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String createConversation(UUID playerUuid, String aiModel, String convName) {
        String convId = UUID.randomUUID().toString();
        String sql = "INSERT INTO conversations(conversation_id, player_uuid, ai_model, conversation_name) VALUES(?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, convId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            pstmt.setString(4, convName);
            pstmt.executeUpdate();
            return convId;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName) {
        String sql = "SELECT 1 FROM conversations WHERE conversation_name = ? AND player_uuid = ? AND ai_model = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, convName);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getConversationId(UUID playerUuid, String aiModel, String convName) {
        String sql = "SELECT conversation_id FROM conversations WHERE conversation_name = ? AND player_uuid = ? AND ai_model = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, convName);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("conversation_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean conversationExists(UUID playerUuid, String aiModel, String convId) {
        String sql = "SELECT 1 FROM conversations WHERE conversation_id = ? AND player_uuid = ? AND ai_model = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, convId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteConversation(UUID playerUuid, String aiModel, String convId) {
        String sqlDeleteHistory = "DELETE FROM chat_history WHERE conversation_id = ? AND player_uuid = ? AND ai_model = ?";
        String sqlDeleteConv = "DELETE FROM conversations WHERE conversation_id = ? AND player_uuid = ? AND ai_model = ?";
        try (PreparedStatement pstmtHistory = connection.prepareStatement(sqlDeleteHistory);
             PreparedStatement pstmtConv = connection.prepareStatement(sqlDeleteConv)) {
            pstmtHistory.setString(1, convId);
            pstmtHistory.setString(2, playerUuid.toString());
            pstmtHistory.setString(3, aiModel);
            pstmtConv.setString(1, convId);
            pstmtConv.setString(2, playerUuid.toString());
            pstmtConv.setString(3, aiModel);
            int rowsAffected = pstmtHistory.executeUpdate() + pstmtConv.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, String> listConversations(UUID playerUuid, String aiModel) {
        Map<String, String> conversations = new HashMap<>();
        String sql = "SELECT conversation_id, conversation_name FROM conversations WHERE player_uuid = ? AND ai_model = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    conversations.put(rs.getString("conversation_id"), rs.getString("conversation_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conversations;
    }

    public void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response) {
        String sql = "INSERT INTO chat_history(player_uuid, ai_model, conversation_id, prompt, response) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, aiModel);
            if (conversationId != null) {
                pstmt.setString(3, conversationId);
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setString(4, prompt);
            pstmt.setString(5, response);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getChatHistory(UUID playerUuid, String aiModel, String conversationId, int maxHistory) {
        StringBuilder history = new StringBuilder();
        String sql = "SELECT prompt, response FROM chat_history " +
                "WHERE player_uuid = ? AND ai_model = ? " +
                (conversationId != null ? "AND conversation_id = ? " : "AND conversation_id IS NULL ") +
                "ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, aiModel);
            int index = 3;
            if (conversationId != null) {
                pstmt.setString(index++, conversationId);
            }
            pstmt.setInt(index, maxHistory);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.insert(0, "User: " + rs.getString("prompt") + "\n");
                history.insert(0, "AI: " + rs.getString("response") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history.toString();
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}