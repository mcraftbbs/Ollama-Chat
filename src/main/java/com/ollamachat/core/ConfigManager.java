
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

    public ConfigManager(Ollamachat plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.selectedConversations = new HashMap<>();
    }

    public void initialize() {
        plugin.saveDefaultConfig();
        reloadConfigValues();
        loadLanguageFile(plugin.getConfig().getString("language", "en_us"));
    }

    private void updateConfig() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("ollama-enabled")) config.set("ollama-enabled", true);
        if (!config.contains("language")) config.set("language", "en");
        if (!config.contains("other-ai-configs")) config.createSection("other-ai-configs");
        if (!config.contains("max-history")) config.set("max-history", 5);
        if (!config.contains("stream-settings")) config.set("stream-settings.enabled", true);
        if (!config.contains("prompts")) config.createSection("prompts");
        if (!config.contains("default-prompt")) config.set("default-prompt", "");
        if (!config.contains("trigger-prefixes")) {
            config.set("trigger-prefixes", Arrays.asList("@bot", "@ai", "/chat"));
        }
        if (!config.contains("suggested-response-models")) {
            config.set("suggested-response-models", Arrays.asList("llama3", "mistral"));
        }
        if (!config.contains("suggested-responses-enabled")) config.set("suggested-responses-enabled", true);

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