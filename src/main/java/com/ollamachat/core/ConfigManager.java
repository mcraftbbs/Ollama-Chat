package com.ollamachat.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConfigManager {
    private final Ollamachat plugin;
    private final Gson gson;
    private JsonObject langConfig;
    private String ollamaApiUrl;
    private String ollamaModel;
    private List<String> triggerPrefixes;
    private int maxResponseLength;
    private Map<String, AIConfig> otherAIConfigs;
    private boolean ollamaEnabled;
    private Map<String, Boolean> otherAIEnabled;
    private boolean streamingEnabled;
    private String defaultPrompt;
    private Map<String, String> prompts;
    private Map<UUID, Map<String, String>> selectedConversations;
    private int maxHistory;
    private List<String> suggestedResponseModels;
    private boolean suggestedResponsesEnabled;
    private int suggestedResponseCount;
    private String suggestedResponsePrompt;
    private List<String> suggestedResponsePresets;
    private Map<String, Boolean> suggestedResponseModelToggles;
    private int suggestedResponseCooldown;
    private boolean suggestedResponsePresetsEnabled;

    public ConfigManager(Ollamachat plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.selectedConversations = new HashMap<>();
        this.suggestedResponseModelToggles = new HashMap<>();
    }

    public void initialize() {
        plugin.saveDefaultConfig();
        reloadConfigValues();
        loadLanguageFile(plugin.getConfig().getString("language", "en_us"));
    }

    private void updateConfig() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("ollama-enabled")) config.set("ollama-enabled", true);
        if (!config.contains("language")) config.set("language", "en_us");
        if (!config.contains("other-ai-configs")) config.createSection("other-ai-configs");
        if (!config.contains("max-history")) config.set("max-history", 5);
        if (!config.contains("stream-settings")) config.set("stream-settings.enabled", true);
        if (!config.contains("prompts")) config.createSection("prompts");
        if (!config.contains("default-prompt")) config.set("default-prompt", "");
        if (!config.contains("trigger-prefixes")) {
            config.set("trigger-prefixes", Arrays.asList("@bot", "@ai"));
        }
        if (!config.contains("suggested-response-models")) {
            config.set("suggested-response-models", Arrays.asList("llama3"));
        }
        if (!config.contains("suggested-responses-enabled")) config.set("suggested-responses-enabled", true);
        if (!config.contains("suggested-response-count")) config.set("suggested-response-count", 3);
        if (!config.contains("suggested-response-prompt")) {
            config.set("suggested-response-prompt", "Conversation:\nUser: {prompt}\nAI: {response}\n\nBased on the above conversation, suggest {count} natural follow-up responses the user might want to say. They should be conversational in tone rather than questions. List them as:\n1. Response 1\n2. Response 2\n3. Response 3");
        }
        if (!config.contains("suggested-response-presets")) {
            config.set("suggested-response-presets", Arrays.asList("I see what you mean.", "That's interesting!", "Tell me more about that."));
        }
        if (!config.contains("suggested-response-model-toggles")) {
            config.createSection("suggested-response-model-toggles");
            for (String model : config.getStringList("suggested-response-models")) {
                if (!config.contains("suggested-response-model-toggles." + model)) {
                    config.set("suggested-response-model-toggles." + model, true);
                }
            }
        }
        if (!config.contains("suggested-response-cooldown")) config.set("suggested-response-cooldown", 10);
        if (!config.contains("suggested-response-presets-enabled")) config.set("suggested-response-presets-enabled", true);
        if (!config.contains("database")) {
            config.set("database.type", "sqlite");
            config.set("database.mysql.host", "localhost");
            config.set("database.mysql.port", 3306);
            config.set("database.mysql.database", "ollamachat");
            config.set("database.mysql.username", "root");
            config.set("database.mysql.password", "");
        }

        plugin.saveConfig();
    }

    public void reloadConfigValues() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        } else {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                if (!config.contains("ollama-api-url") || !config.contains("model")) {
                    plugin.getLogger().warning(getMessage("config-invalid", null));
                    configFile.delete();
                    plugin.saveDefaultConfig();
                }
            } catch (Exception e) {
                plugin.getLogger().severe(getMessage("config-load-failed", Map.of("error", e.getMessage())));
                configFile.delete();
                plugin.saveDefaultConfig();
            }
        }

        plugin.reloadConfig();
        updateConfig();

        FileConfiguration config = plugin.getConfig();
        ollamaApiUrl = config.getString("ollama-api-url", "http://localhost:11434/api/generate");
        ollamaModel = config.getString("model", "llama3");
        triggerPrefixes = config.getStringList("trigger-prefixes");
        maxResponseLength = config.getInt("max-response-length", 500);
        ollamaEnabled = config.getBoolean("ollama-enabled", true);
        maxHistory = config.getInt("max-history", 5);
        streamingEnabled = config.getBoolean("stream-settings.enabled", true);
        defaultPrompt = config.getString("default-prompt", "");
        suggestedResponseModels = config.getStringList("suggested-response-models");
        suggestedResponsesEnabled = config.getBoolean("suggested-responses-enabled", true);
        suggestedResponseCount = config.getInt("suggested-response-count", 3);
        suggestedResponsePrompt = config.getString("suggested-response-prompt", "Conversation:\nUser: {prompt}\nAI: {response}\n\nBased on the above conversation, suggest {count} natural follow-up responses the user might want to say. They should be conversational in tone rather than questions. List them as:\n1. Response 1\n2. Response 2\n3. Response 3");
        suggestedResponsePresets = config.getStringList("suggested-response-presets");
        suggestedResponseCooldown = config.getInt("suggested-response-cooldown", 10);
        suggestedResponsePresetsEnabled = config.getBoolean("suggested-response-presets-enabled", true);

        prompts = new HashMap<>();
        if (config.contains("prompts") && config.getConfigurationSection("prompts") != null) {
            for (String promptName : config.getConfigurationSection("prompts").getKeys(false)) {
                String promptContent = config.getString("prompts." + promptName);
                prompts.put(promptName, promptContent);
            }
        }

        otherAIConfigs = new HashMap<>();
        otherAIEnabled = new HashMap<>();
        if (config.contains("other-ai-configs") && config.getConfigurationSection("other-ai-configs") != null) {
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

        suggestedResponseModelToggles = new HashMap<>();
        if (config.contains("suggested-response-model-toggles") && config.getConfigurationSection("suggested-response-model-toggles") != null) {
            for (String model : config.getConfigurationSection("suggested-response-model-toggles").getKeys(false)) {
                suggestedResponseModelToggles.put(model, config.getBoolean("suggested-response-model-toggles." + model, true));
            }
        }
    }

    private void loadLanguageFile(String language) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File langFile = new File(langFolder, language + ".json");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + language + ".json", false);
        }

        try (FileReader reader = new FileReader(langFile)) {
            langConfig = gson.fromJson(reader, JsonObject.class);
            if (langConfig == null) {
                langConfig = new JsonObject();
                plugin.getLogger().warning("Language file is empty or invalid: " + langFile.getName());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load language file: " + langFile.getName() + " - " + e.getMessage());
            langConfig = new JsonObject();
        }
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = langConfig != null && langConfig.has(key) ? langConfig.get(key).getAsString() : "§c[OllamaChat] Missing language key: " + key;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public boolean isSuggestedResponsePresetsEnabled() {
        return suggestedResponsePresetsEnabled;
    }

    public void setSuggestedResponsePresetsEnabled(boolean enabled) {
        this.suggestedResponsePresetsEnabled = enabled;
        plugin.getConfig().set("suggested-response-presets-enabled", enabled);
        plugin.saveConfig();
    }

    // Getters
    public String getOllamaApiUrl() {
        return ollamaApiUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public List<String> getTriggerPrefixes() {
        return triggerPrefixes;
    }

    public int getMaxResponseLength() {
        return maxResponseLength;
    }

    public Map<String, AIConfig> getOtherAIConfigs() {
        return otherAIConfigs;
    }

    public boolean isOllamaEnabled() {
        return ollamaEnabled;
    }

    public void setOllamaEnabled(boolean enabled) {
        this.ollamaEnabled = enabled;
    }

    public Map<String, Boolean> getOtherAIEnabled() {
        return otherAIEnabled;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public String getDefaultPrompt() {
        return defaultPrompt;
    }

    public void setDefaultPrompt(String prompt) {
        this.defaultPrompt = prompt;
    }

    public Map<String, String> getPrompts() {
        return prompts;
    }

    public Map<UUID, Map<String, String>> getSelectedConversations() {
        return selectedConversations;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public List<String> getSuggestedResponseModels() {
        return suggestedResponseModels;
    }

    public boolean isSuggestedResponsesEnabled() {
        return suggestedResponsesEnabled;
    }

    public int getSuggestedResponseCount() {
        return suggestedResponseCount;
    }

    public String getSuggestedResponsePrompt() {
        return suggestedResponsePrompt;
    }

    public List<String> getSuggestedResponsePresets() {
        return suggestedResponsePresets;
    }

    public Map<String, Boolean> getSuggestedResponseModelToggles() {
        return suggestedResponseModelToggles;
    }

    public int getSuggestedResponseCooldown() {
        return suggestedResponseCooldown;
    }

    public static class AIConfig {
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
}


