package com.ollamachat;

import java.sql.*;
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

            stmt.execute("CREATE TABLE IF NOT EXISTS chat_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "ai_model TEXT NOT NULL," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "prompt TEXT NOT NULL," +
                    "response TEXT NOT NULL," +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid))");
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

    public void saveChatHistory(UUID playerUuid, String aiModel, String prompt, String response) {
        String sql = "INSERT INTO chat_history(player_uuid, ai_model, prompt, response) VALUES(?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, aiModel);
            pstmt.setString(3, prompt);
            pstmt.setString(4, response);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getChatHistory(UUID playerUuid, String aiModel, int maxHistory) {
        StringBuilder history = new StringBuilder();
        String sql = "SELECT prompt, response FROM chat_history " +
                "WHERE player_uuid = ? AND ai_model = ? " +
                "ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, aiModel);
            pstmt.setInt(3, maxHistory);
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
