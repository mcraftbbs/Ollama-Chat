
package com.ollamachat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private String databaseType; // "sqlite" or "mysql"
    private Connection sqliteConnection; // For SQLite, maintain a single connection
    private final ClassLoader dependencyClassLoader;
    private String mysqlUrl;
    private String mysqlUsername;
    private String mysqlPassword;

    public DatabaseManager(JavaPlugin plugin, ClassLoader dependencyClassLoader) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dependencyClassLoader = dependencyClassLoader;
        initializeDatabase();
    }

    private void initializeDatabase() {
        FileConfiguration config = plugin.getConfig();
        databaseType = config.getString("database.type", "sqlite").toLowerCase();

        try {
            if (databaseType.equals("mysql")) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }
            createTables();
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void initializeSQLite() {
        try {
            // Use the dependency class loader to load SQLite driver
            Class.forName("org.sqlite.JDBC", true, dependencyClassLoader);
        } catch (ClassNotFoundException e) {
            logger.severe("SQLite JDBC driver not found. Ensure 'sqlite-jdbc' dependency is included in the plugin.");
            throw new RuntimeException("SQLite driver not found", e);
        }

        try {
            databaseType = "sqlite";
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:plugins/OllamaChat/chat_history.db");
            sqliteConnection.setAutoCommit(true);
            logger.info("SQLite database initialized successfully.");
        } catch (SQLException e) {
            logger.severe("Failed to initialize SQLite database: " + e.getMessage());
            throw new RuntimeException("SQLite initialization failed", e);
        }
    }

    private void initializeMySQL() {
        try {
            // Use the dependency class loader to load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver", true, dependencyClassLoader);
        } catch (ClassNotFoundException e) {
            logger.severe("MySQL JDBC driver not found. Ensure 'mysql-connector-java' dependency is included in the plugin.");
            logger.warning("Falling back to SQLite due to missing MySQL driver.");
            initializeSQLite();
            return;
        }

        try {
            FileConfiguration config = plugin.getConfig();
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "ollamachat");
            mysqlUsername = config.getString("database.mysql.username", "root");
            mysqlPassword = config.getString("database.mysql.password", "");
            mysqlUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true", host, port, database);

            // Test connection
            try (Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword)) {
                logger.info("MySQL database initialized successfully.");
            }
        } catch (SQLException e) {
            logger.severe("Failed to initialize MySQL database: " + e.getMessage());
            logger.warning("Falling back to SQLite due to MySQL initialization failure.");
            initializeSQLite();
        }
    }

    private Connection getConnection() throws SQLException {
        if (databaseType.equals("sqlite")) {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                sqliteConnection = DriverManager.getConnection("jdbc:sqlite:plugins/OllamaChat/chat_history.db");
                sqliteConnection.setAutoCommit(true);
            }
            return sqliteConnection;
        } else {
            return DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword);
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
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
                    "id INTEGER PRIMARY KEY " + (databaseType.equals("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + "," +
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
        String sql;
        if (databaseType.equals("sqlite")) {
            sql = "INSERT OR REPLACE INTO players (uuid, username) VALUES (?, ?)";
        } else {
            sql = "INSERT INTO players (uuid, username) VALUES (?, ?) ON DUPLICATE KEY UPDATE username = ?";
        }
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, username);
            if (databaseType.equals("mysql")) {
                pstmt.setString(3, username); // For ON DUPLICATE KEY UPDATE
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save player info: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String createConversation(UUID playerUuid, String aiModel, String convName) {
        String convId = UUID.randomUUID().toString();
        String sql = "INSERT INTO conversations (conversation_id, player_uuid, ai_model, conversation_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, convId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            pstmt.setString(4, convName);
            pstmt.executeUpdate();
            return convId;
        } catch (SQLException e) {
            logger.severe("Failed to create conversation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean conversationExistsByName(UUID playerUuid, String aiModel, String convName) {
        String sql = "SELECT 1 FROM conversations WHERE conversation_name = ? AND player_uuid = ? AND ai_model = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, convName);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("Failed to check conversation existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String getConversationId(UUID playerUuid, String aiModel, String convName) {
        String sql = "SELECT conversation_id FROM conversations WHERE conversation_name = ? AND player_uuid = ? AND ai_model = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, convName);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("conversation_id");
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get conversation ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean conversationExists(UUID playerUuid, String aiModel, String convId) {
        String sql = "SELECT 1 FROM conversations WHERE conversation_id = ? AND player_uuid = ? AND ai_model = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, convId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("Failed to check conversation existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteConversation(UUID playerUuid, String aiModel, String convId) {
        String sqlDeleteHistory = "DELETE FROM chat_history WHERE conversation_id = ? AND player_uuid = ? AND ai_model = ?";
        String sqlDeleteConv = "DELETE FROM conversations WHERE conversation_id = ? AND player_uuid = ? AND ai_model = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmtHistory = conn.prepareStatement(sqlDeleteHistory);
             PreparedStatement pstmtConv = conn.prepareStatement(sqlDeleteConv)) {
            pstmtHistory.setString(1, convId);
            pstmtHistory.setString(2, playerUuid.toString());
            pstmtHistory.setString(3, aiModel);
            pstmtConv.setString(1, convId);
            pstmtConv.setString(2, playerUuid.toString());
            pstmtConv.setString(3, aiModel);
            int rowsAffected = pstmtHistory.executeUpdate() + pstmtConv.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.severe("Failed to delete conversation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, String> listConversations(UUID playerUuid, String aiModel) {
        Map<String, String> conversations = new HashMap<>();
        String sql = "SELECT conversation_id, conversation_name FROM conversations WHERE player_uuid = ? AND ai_model = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, aiModel);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    conversations.put(rs.getString("conversation_id"), rs.getString("conversation_name"));
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to list conversations: " + e.getMessage());
            e.printStackTrace();
        }
        return conversations;
    }

    public void saveChatHistory(UUID playerUuid, String aiModel, String conversationId, String prompt, String response) {
        String sql = "INSERT INTO chat_history (player_uuid, ai_model, conversation_id, prompt, response) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
            logger.severe("Failed to save chat history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getChatHistory(UUID playerUuid, String aiModel, String conversationId, int maxHistory) {
        StringBuilder history = new StringBuilder();
        String sql = "SELECT prompt, response FROM chat_history " +
                "WHERE player_uuid = ? AND ai_model = ? " +
                (conversationId != null ? "AND conversation_id = ? " : "AND conversation_id IS NULL ") +
                "ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, aiModel);
            int index = 3;
            if (conversationId != null) {
                pstmt.setString(index++, conversationId);
            }
            pstmt.setInt(index, maxHistory);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.insert(0, "User: " + rs.getString("prompt") + "\n");
                    history.insert(0, "AI: " + rs.getString("response") + "\n");
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get chat history: " + e.getMessage());
            e.printStackTrace();
        }
        return history.toString();
    }

    public void close() {
        try {
            if (databaseType.equals("sqlite") && sqliteConnection != null && !sqliteConnection.isClosed()) {
                sqliteConnection.close();
            }
            // No need to close MySQL connections explicitly as they are managed per query
        } catch (SQLException e) {
            logger.severe("Failed to close database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}







