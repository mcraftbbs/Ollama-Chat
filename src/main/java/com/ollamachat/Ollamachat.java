package com.ollamachat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private boolean streamingEnabled;
    private String defaultPrompt;
    private Map<String, String> prompts;

    private FileConfiguration langConfig;
    private DatabaseManager databaseManager;
    private ChatHistoryManager chatHistoryManager;
    private int maxHistory;
    private ProgressManager progressManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();
        String language = getConfig().getString("language", "en");
        loadLanguageFile(language);

        updateCommandUsages();

        databaseManager = new DatabaseManager();
        maxHistory = getConfig().getInt("max-history", 5);
        chatHistoryManager = new ChatHistoryManager(databaseManager, maxHistory);

        aiService = new AIService();
        gson = new Gson();
        progressManager = new ProgressManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ollamachat").setExecutor(this);
        getCommand("aichat").setExecutor(this);
    }

    private void updateCommandUsages() {
        String usageOllamachat = getMessage("usage-ollamachat", null);
        String usageAichat = getMessage("usage-aichat", null);

        getCommand("ollamachat").setUsage(usageOllamachat);
        getCommand("aichat").setUsage(usageAichat);
    }

    @Override
    public void onDisable() {
        databaseManager.close();
        Bukkit.getOnlinePlayers().forEach(progressManager::cleanup);
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
        if (!config.contains("stream-settings")) {
            config.set("stream-settings.enabled", true);
        }
        if (!config.contains("prompts")) {
            config.createSection("prompts");
        }
        if (!config.contains("default-prompt")) {
            config.set("default-prompt", "");
        }

        saveConfig();
    }

    private void reloadConfigValues() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        } else {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                if (!config.contains("ollama-api-url") || !config.contains("model")) {
                    getLogger().warning(getMessage("config-invalid", null));
                    configFile.delete();
                    saveDefaultConfig();
                }
            } catch (Exception e) {
                getLogger().severe(getMessage("config-load-failed", Map.of("error", e.getMessage())));
                configFile.delete();
                saveDefaultConfig();
            }
        }

        reloadConfig();
        updateConfig();

        FileConfiguration config = getConfig();
        ollamaApiUrl = config.getString("ollama-api-url", "http://localhost:11434/api/generate");
        ollamaModel = config.getString("model", "llama3");
        triggerPrefix = config.getString("trigger-prefix", "@bot ");
        maxResponseLength = config.getInt("max-response-length", 500);
        ollamaEnabled = config.getBoolean("ollama-enabled", true);
        maxHistory = config.getInt("max-history", 5);
        streamingEnabled = config.getBoolean("stream-settings.enabled", true);
        defaultPrompt = config.getString("default-prompt", "");

        prompts = new HashMap<>();
        if (config.contains("prompts")) {
            for (String promptName : config.getConfigurationSection("prompts").getKeys(false)) {
                String promptContent = config.getString("prompts." + promptName);
                prompts.put(promptName, promptContent);
            }
        }

        otherAIConfigs = new HashMap<>();
        otherAIEnabled = new HashMap<>();
        if (config.contains("other-ai-configs")) {
            for (String aiName : config.getConfigurationSection("other-ai-configs").getKeys(false)) {
                String apiUrl = config.getString("other-ai-configs." + aiName + ".api-url");
                String apiKey = config.getString("other-ai-configs." + aiName + ".api-key");
                String model = config.getString("other-ai-configs." + aiName + ".model");
                boolean enabled = config.getBoolean("other-ai-configs." + aiName + ".enabled", true);
                boolean isMessagesFormat = config.getBoolean("other-ai-configs." + aiName + ".messages-format", false);
                otherAIConfigs.put(aiName, new AIConfig(apiUrl, apiKey, model, isMessagesFormat));
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

    String getMessage(String key, Map<String, String> placeholders) {
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
        if (getConfig().getBoolean("progress-display.enabled", true)) {
            BarColor color = BarColor.valueOf(getConfig().getString("progress-display.color", "BLUE"));
            BarStyle style = BarStyle.valueOf(getConfig().getString("progress-display.style", "SOLID"));
            progressManager.startProgress(player,
                    getConfig().getString("progress-display.title", "Generating..."),
                    color,
                    style
            );
        }

        CompletableFuture.runAsync(() -> {
            try {
                String history = chatHistoryManager.getChatHistory(player.getUniqueId(), "ollama");
                String selectedPrompt = prompts.getOrDefault(defaultPrompt, "");
                String context = history + (selectedPrompt.isEmpty() ? "" : selectedPrompt + "\n") + "User: " + prompt;

                String finalResponse;
                if (streamingEnabled) {
                    StringBuilder fullResponse = new StringBuilder();
                    AtomicBoolean isFirstMessage = new AtomicBoolean(true);
                    aiService.sendStreamingRequest(ollamaApiUrl, null, ollamaModel, context, partialResponse -> {
                        if (player.isOnline()) {
                            String formattedPartial = partialResponse.length() > maxResponseLength
                                    ? partialResponse.substring(0, maxResponseLength) + "..."
                                    : partialResponse;
                            String message = isFirstMessage.get()
                                    ? getMessage("response-prefix", null) + formattedPartial
                                    : formattedPartial;
                            player.sendMessage(message);
                            isFirstMessage.set(false);
                            fullResponse.append(partialResponse);
                        }
                    }, false).join();
                    finalResponse = fullResponse.toString();
                } else {
                    String responseBody = aiService.sendRequest(ollamaApiUrl, null, ollamaModel, context, false).join();
                    OllamaResponse ollamaResponse = gson.fromJson(responseBody, OllamaResponse.class);
                    finalResponse = ollamaResponse.response;
                    if (player.isOnline()) {
                        sendFormattedResponse(player, finalResponse);
                    }
                }

                if (!finalResponse.isEmpty()) {
                    chatHistoryManager.saveChatHistory(player.getUniqueId(), "ollama", prompt, finalResponse);
                }
                progressManager.complete(player);
            } catch (Exception e) {
                getLogger().severe("Error processing Ollama request: " + e.getMessage());
                if (player.isOnline()) {
                    sendErrorMessage(player, getMessage("error-prefix", null) + "Failed to get response from Ollama");
                }
                progressManager.error(player);
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
                String selectedPrompt = prompts.getOrDefault(defaultPrompt, "");
                String context = history + (selectedPrompt.isEmpty() ? "" : selectedPrompt + "\n") + "User: " + prompt;

                AIConfig aiConfig = otherAIConfigs.get(aiName);
                String responseBody = aiService.sendRequest(
                        aiConfig.getApiUrl(),
                        aiConfig.getApiKey(),
                        aiConfig.getModel(),
                        context,
                        aiConfig.isMessagesFormat()
                ).join();

                String response = parseAIResponse(aiName, responseBody, aiConfig.isMessagesFormat());

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

    private String parseAIResponse(String aiName, String responseBody, boolean isMessagesFormat) {
        if (isMessagesFormat) {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .get("message").getAsJsonObject()
                    .get("content").getAsString();
        } else {
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
            if (args.length == 0) {
                sender.sendMessage(getMessage("usage-ollamachat", null));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("ollamachat.reload")) {
                    sender.sendMessage(getMessage("no-permission", null));
                    return true;
                }
                reloadConfigValues();
                loadLanguageFile(getConfig().getString("language", "en"));
                sender.sendMessage(getMessage("reload-success", null));
                return true;
            } else if (args[0].equalsIgnoreCase("toggle") && args.length > 1) {
                if (!sender.hasPermission("ollamachat.toggle")) {
                    sender.sendMessage(getMessage("no-permission", null));
                    return true;
                }
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
            } else if (args[0].equalsIgnoreCase("prompt") && args.length > 1) {
                String subCommand = args[1].toLowerCase();
                if (subCommand.equals("set") && args.length > 3) {
                    if (!sender.hasPermission("ollamachat.prompt.set")) {
                        sender.sendMessage(getMessage("no-permission", null));
                        return true;
                    }
                    String promptName = args[2];
                    String promptContent = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                    getConfig().set("prompts." + promptName, promptContent);
                    saveConfig();
                    prompts.put(promptName, promptContent);
                    sender.sendMessage(getMessage("prompt-set", Map.of("name", promptName)));
                    return true;
                } else if (subCommand.equals("delete") && args.length == 3) {
                    if (!sender.hasPermission("ollamachat.prompt.delete")) {
                        sender.sendMessage(getMessage("no-permission", null));
                        return true;
                    }
                    String promptName = args[2];
                    if (prompts.containsKey(promptName)) {
                        getConfig().set("prompts." + promptName, null);
                        if (defaultPrompt.equals(promptName)) {
                            getConfig().set("default-prompt", "");
                            defaultPrompt = "";
                        }
                        saveConfig();
                        prompts.remove(promptName);
                        sender.sendMessage(getMessage("prompt-deleted", Map.of("name", promptName)));
                    } else {
                        sender.sendMessage(getMessage("prompt-not-found", Map.of("name", promptName)));
                    }
                    return true;
                } else if (subCommand.equals("list")) {
                    if (!sender.hasPermission("ollamachat.prompt.list")) {
                        sender.sendMessage(getMessage("no-permission", null));
                        return true;
                    }
                    if (prompts.isEmpty()) {
                        sender.sendMessage(getMessage("prompt-list-empty", null));
                    } else {
                        sender.sendMessage(getMessage("prompt-list", Map.of("prompts", String.join(", ", prompts.keySet()))));
                    }
                    if (!defaultPrompt.isEmpty() && prompts.containsKey(defaultPrompt)) {
                        sender.sendMessage(getMessage("prompt-default", Map.of("name", defaultPrompt)));
                    } else if (!defaultPrompt.isEmpty()) {
                        sender.sendMessage(getMessage("prompt-default-invalid", Map.of("name", defaultPrompt)));
                    }
                    return true;
                } else if (subCommand.equals("select") && args.length == 3) {
                    if (!sender.hasPermission("ollamachat.prompt.select")) {
                        sender.sendMessage(getMessage("no-permission", null));
                        return true;
                    }
                    String promptName = args[2];
                    if (prompts.containsKey(promptName)) {
                        getConfig().set("default-prompt", promptName);
                        saveConfig();
                        defaultPrompt = promptName;
                        sender.sendMessage(getMessage("prompt-selected", Map.of("name", promptName)));
                    } else {
                        sender.sendMessage(getMessage("prompt-not-found", Map.of("name", promptName)));
                    }
                    return true;
                } else if (subCommand.equals("clear")) {
                    if (!sender.hasPermission("ollamachat.prompt.select")) {
                        sender.sendMessage(getMessage("no-permission", null));
                        return true;
                    }
                    getConfig().set("default-prompt", "");
                    saveConfig();
                    defaultPrompt = "";
                    sender.sendMessage(getMessage("prompt-cleared", null));
                    return true;
                } else {
                    sender.sendMessage(getMessage("prompt-usage", null));
                    return true;
                }
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
        private final boolean isMessagesFormat;

        public AIConfig(String apiUrl, String apiKey, String model, boolean isMessagesFormat) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.isMessagesFormat = isMessagesFormat;
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

        public boolean isMessagesFormat() {
            return isMessagesFormat;
        }
    }

    private static class OllamaResponse {
        public String response;
    }
}

