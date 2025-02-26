package com.ollamachat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Ollamachat extends JavaPlugin implements Listener {

    private AIService aiService;
    private Gson gson;
    private String ollamaApiUrl;
    private String ollamaModel;
    private String triggerPrefix;
    private int maxResponseLength;
    private Map<String, AIConfig> otherAIConfigs;
    private boolean ollamaEnabled;
    private Map<String, Boolean> otherAIEnabled;

    private FileConfiguration langConfig;
    private DatabaseManager databaseManager;
    private ChatHistoryManager chatHistoryManager;
    private int maxHistory;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();
        String language = getConfig().getString("language", "en");
        loadLanguageFile(language);

        databaseManager = new DatabaseManager();
        maxHistory = getConfig().getInt("max-history", 5);
        chatHistoryManager = new ChatHistoryManager(databaseManager, maxHistory);

        aiService = new AIService();
        gson = new Gson();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ollamachat").setExecutor(this);
        getCommand("aichat").setExecutor(this);
    }

    @Override
    public void onDisable() {
        databaseManager.close();
    }

    private void updateConfig() {
        FileConfiguration config = getConfig();

        if (!config.contains("ollama-enabled")) {
            config.set("ollama-enabled", true);
        }
        if (!config.contains("language")) {
            config.set("language", "en");
        }
        if (!config.contains("other-ai-configs")) {
            config.createSection("other-ai-configs");
        }
        if (!config.contains("max-history")) {
            config.set("max-history", 5);
        }

        saveConfig();
    }

    private void reloadConfigValues() {
        reloadConfig();
        updateConfig();

        FileConfiguration config = getConfig();
        ollamaApiUrl = config.getString("ollama-api-url", "http://localhost:11434/api/generate");
        ollamaModel = config.getString("model", "llama3");
        triggerPrefix = config.getString("trigger-prefix", "@bot ");
        maxResponseLength = config.getInt("max-response-length", 500);
        ollamaEnabled = config.getBoolean("ollama-enabled", true);
        maxHistory = config.getInt("max-history", 5);

        otherAIConfigs = new HashMap<>();
        otherAIEnabled = new HashMap<>();
        if (config.contains("other-ai-configs")) {
            for (String aiName : config.getConfigurationSection("other-ai-configs").getKeys(false)) {
                String apiUrl = config.getString("other-ai-configs." + aiName + ".api-url");
                String apiKey = config.getString("other-ai-configs." + aiName + ".api-key");
                String model = config.getString("other-ai-configs." + aiName + ".model");
                boolean enabled = config.getBoolean("other-ai-configs." + aiName + ".enabled", true);
                otherAIConfigs.put(aiName, new AIConfig(apiUrl, apiKey, model));
                otherAIEnabled.put(aiName, enabled);
            }
        }
    }

    private void loadLanguageFile(String language) {
        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File langFile = new File(langFolder, language + ".lang");
        if (!langFile.exists()) {
            saveResource("lang/" + language + ".lang", false);
        }

        try {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        } catch (Exception e) {
            getLogger().severe("Failed to load language file: " + langFile.getName());
            e.printStackTrace();
        }
    }

    private String getMessage(String key, Map<String, String> placeholders) {
        String message = langConfig.getString(key, "§cMissing language key: " + key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        chatHistoryManager.savePlayerInfo(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!ollamaEnabled) return;

        String message = event.getMessage();
        Player player = event.getPlayer();

        if (message.startsWith(triggerPrefix)) {
            event.setCancelled(true);
            String prompt = message.substring(triggerPrefix.length()).trim();

            if (!prompt.isEmpty()) {
                processOllamaQueryAsync(player, prompt);
            }
        }
    }

    private void processOllamaQueryAsync(Player player, String prompt) {
        CompletableFuture.runAsync(() -> {
            try {
                String history = chatHistoryManager.getChatHistory(player.getUniqueId(), "ollama");

                String context = history + "User: " + prompt;

                String responseBody = aiService.sendRequest(ollamaApiUrl, null, ollamaModel, context).join();
                OllamaResponse ollamaResponse = gson.fromJson(responseBody, OllamaResponse.class);

                chatHistoryManager.saveChatHistory(player.getUniqueId(), "ollama", prompt, ollamaResponse.response);

                sendFormattedResponse(player, ollamaResponse.response);
            } catch (Exception e) {
                getLogger().severe("Error processing Ollama request: " + e.getMessage());
                sendErrorMessage(player, getMessage("error-prefix", null) + "Failed to get response from Ollama");
            }
        });
    }

    private void processOtherAIQueryAsync(Player player, String aiName, String prompt) {
        if (!otherAIEnabled.getOrDefault(aiName, false)) {
            sendErrorMessage(player, getMessage("error-prefix", null) + getMessage("toggle-disabled", Map.of("ai-name", aiName)));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String history = chatHistoryManager.getChatHistory(player.getUniqueId(), aiName);

                String context = history + "User: " + prompt;

                AIConfig aiConfig = otherAIConfigs.get(aiName);
                String responseBody = aiService.sendRequest(
                        aiConfig.getApiUrl(),
                        aiConfig.getApiKey(),
                        aiConfig.getModel(),
                        context
                ).join();

                String response = parseAIResponse(aiName, responseBody);

                chatHistoryManager.saveChatHistory(
                        player.getUniqueId(),
                        aiName,
                        prompt,
                        response
                );

                sendFormattedResponse(player, response);
            } catch (Exception e) {
                getLogger().severe("Error processing " + aiName + " request: " + e.getMessage());
                sendErrorMessage(player, getMessage("error-prefix", null) + "Failed to get response from " + aiName);
            }
        });
    }

    // 新增方法：解析不同AI的响应
    private String parseAIResponse(String aiName, String responseBody) {
        switch (aiName.toLowerCase()) {
            case "openai":
                // OpenAI 响应结构示例：{"choices": [{"message": {"content": "..."}}]}
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                return json.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .get("message").getAsJsonObject()
                        .get("content").getAsString();
            default:
                // 默认解析为 Ollama 格式
                return gson.fromJson(responseBody, OllamaResponse.class).response;
        }
    }

    private void sendFormattedResponse(Player player, String response) {
        if (response.length() > maxResponseLength) {
            response = response.substring(0, maxResponseLength) + "...";
        }
        player.sendMessage(getMessage("response-prefix", null) + response);
    }

    private void sendErrorMessage(Player player, String errorMessage) {
        player.sendMessage(getMessage("error-prefix", null) + errorMessage);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ollamachat")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfigValues();
                loadLanguageFile(getConfig().getString("language", "en"));
                sender.sendMessage(getMessage("reload-success", null));
                return true;
            } else if (args.length > 1 && args[0].equalsIgnoreCase("toggle")) {
                String aiName = args[1];
                if (aiName.equalsIgnoreCase("ollama")) {
                    ollamaEnabled = !ollamaEnabled;
                    sender.sendMessage(getMessage(ollamaEnabled ? "ollama-enabled" : "ollama-disabled", null));
                } else if (otherAIConfigs.containsKey(aiName)) {
                    boolean newState = !otherAIEnabled.getOrDefault(aiName, false);
                    otherAIEnabled.put(aiName, newState);
                    sender.sendMessage(getMessage(newState ? "toggle-enabled" : "toggle-disabled", Map.of("ai-name", aiName)));
                } else {
                    sender.sendMessage(getMessage("invalid-ai-name", Map.of("ai-list", String.join(", ", otherAIConfigs.keySet()))));
                }
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("aichat")) {
            if (args.length < 2) {
                sender.sendMessage(getMessage("usage-aichat", null));
                return true;
            }

            String aiName = args[0];
            String prompt = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

            if (otherAIConfigs.containsKey(aiName)) {
                if (sender instanceof Player) {
                    processOtherAIQueryAsync((Player) sender, aiName, prompt);
                } else {
                    sender.sendMessage(getMessage("player-only", null));
                }
            } else {
                sender.sendMessage(getMessage("invalid-ai-name", Map.of("ai-list", String.join(", ", otherAIConfigs.keySet()))));
            }
            return true;
        }
        return false;
    }

    private static class AIConfig {
        private final String apiUrl;
        private final String apiKey;
        private final String model;

        public AIConfig(String apiUrl, String apiKey, String model) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.model = model;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getModel() {
            return model;
        }
    }

    private static class OllamaResponse {
        public String response;
    }
}