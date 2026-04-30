package com.ollamachat.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ConfigManager {
    private final Ollamachat plugin;
    private final Gson gson;
    private JsonObject langConfig;
    private String currentLanguage;

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

    private boolean webSearchEnabled;
    private boolean webSearchAutoTrigger;
    private List<String> webSearchTriggerKeywords;
    private int webSearchResultCount;
    private SearchEngine webSearchEngine;
    private String webSearchPromptTemplate;

    private String bochaApiKey;
    private boolean bochaIncludeSites;
    private List<String> bochaIncludeSitesList;
    private boolean bochaExcludeSites;
    private List<String> bochaExcludeSitesList;
    private int bochaTimeRange;
    private String bochaFreshness;

    private String braveApiKey;
    private String braveCountry;
    private String braveSearchLang;
    private String braveUiLang;
    private String braveSafeSearch;

    public enum SearchEngine {
        BOCHA("bocha"),
        BRAVE("brave");

        private final String configName;

        SearchEngine(String configName) {
            this.configName = configName;
        }

        public String getConfigName() {
            return configName;
        }

        public static SearchEngine fromString(String name) {
            for (SearchEngine engine : values()) {
                if (engine.configName.equalsIgnoreCase(name) || engine.name().equalsIgnoreCase(name)) {
                    return engine;
                }
            }
            return BRAVE;
        }
    }

    private static final String DEFAULT_OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3";
    private static final List<String> DEFAULT_TRIGGER_PREFIXES = Arrays.asList("@bot", "@ai");
    private static final int DEFAULT_MAX_RESPONSE_LENGTH = 500;
    private static final int DEFAULT_MAX_HISTORY = 5;
    private static final int DEFAULT_SUGGESTED_RESPONSE_COUNT = 3;
    private static final int DEFAULT_SUGGESTED_RESPONSE_COOLDOWN = 10;
    private static final boolean DEFAULT_STREAMING_ENABLED = true;
    private static final boolean DEFAULT_OLLAMA_ENABLED = true;
    private static final boolean DEFAULT_SUGGESTED_RESPONSES_ENABLED = true;
    private static final boolean DEFAULT_SUGGESTED_RESPONSE_PRESETS_ENABLED = true;

    private static final boolean DEFAULT_WEB_SEARCH_ENABLED = false;
    private static final boolean DEFAULT_WEB_SEARCH_AUTO_TRIGGER = true;
    private static final List<String> DEFAULT_WEB_SEARCH_TRIGGER_KEYWORDS = Arrays.asList(
            "search", "find", "look up", "google", "what is", "who is", "when did", "where is"
    );
    private static final int DEFAULT_WEB_SEARCH_RESULT_COUNT = 5;
    private static final SearchEngine DEFAULT_WEB_SEARCH_ENGINE = SearchEngine.BRAVE;

    private static final String DEFAULT_BOCHA_API_KEY = "";
    private static final boolean DEFAULT_BOCHA_INCLUDE_SITES = false;
    private static final List<String> DEFAULT_BOCHA_INCLUDE_SITES_LIST = new ArrayList<>();
    private static final boolean DEFAULT_BOCHA_EXCLUDE_SITES = false;
    private static final List<String> DEFAULT_BOCHA_EXCLUDE_SITES_LIST = new ArrayList<>();
    private static final int DEFAULT_BOCHA_TIME_RANGE = 0;
    private static final String DEFAULT_BOCHA_FRESHNESS = "";

    private static final String DEFAULT_BRAVE_API_KEY = "";
    private static final String DEFAULT_BRAVE_COUNTRY = "US";
    private static final String DEFAULT_BRAVE_SEARCH_LANG = "en";
    private static final String DEFAULT_BRAVE_UI_LANG = "en";
    private static final String DEFAULT_BRAVE_SAFE_SEARCH = "moderate";

    private static final String DEFAULT_WEB_SEARCH_PROMPT_TEMPLATE =
            "Based on the following search results, please answer the user's question:\n\n" +
                    "{search_results}\n\n" +
                    "User question: {prompt}\n\n" +
                    "Please provide an accurate and detailed answer based on the search results. " +
                    "If the search results are insufficient, please indicate that.";

    public ConfigManager(Ollamachat plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.selectedConversations = new HashMap<>();
        this.suggestedResponseModelToggles = new HashMap<>();
        this.otherAIEnabled = new HashMap<>();
        this.prompts = new HashMap<>();

        this.webSearchTriggerKeywords = new ArrayList<>(DEFAULT_WEB_SEARCH_TRIGGER_KEYWORDS);
        this.bochaIncludeSitesList = new ArrayList<>();
        this.bochaExcludeSitesList = new ArrayList<>();
    }

    public void initialize() {
        createDefaultConfig();
        ensureConfigComplete();
        reloadConfigValues();
        loadLanguageFile(getCurrentLanguageFromConfig());
    }

    private void createDefaultConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            setDefaultConfigValues();
        }
    }

    private void setDefaultConfigValues() {
        FileConfiguration config = plugin.getConfig();

        config.addDefault("language", "en_us");
        config.addDefault("ollama-enabled", DEFAULT_OLLAMA_ENABLED);
        config.addDefault("ollama-api-url", DEFAULT_OLLAMA_API_URL);
        config.addDefault("model", DEFAULT_OLLAMA_MODEL);
        config.addDefault("max-history", DEFAULT_MAX_HISTORY);
        config.addDefault("max-response-length", DEFAULT_MAX_RESPONSE_LENGTH);
        config.addDefault("trigger-prefixes", DEFAULT_TRIGGER_PREFIXES);

        config.addDefault("stream-settings.enabled", DEFAULT_STREAMING_ENABLED);

        config.addDefault("suggested-response-models", Arrays.asList(DEFAULT_OLLAMA_MODEL));
        config.addDefault("suggested-responses-enabled", DEFAULT_SUGGESTED_RESPONSES_ENABLED);
        config.addDefault("suggested-response-count", DEFAULT_SUGGESTED_RESPONSE_COUNT);
        config.addDefault("suggested-response-cooldown", DEFAULT_SUGGESTED_RESPONSE_COOLDOWN);
        config.addDefault("suggested-response-presets-enabled", DEFAULT_SUGGESTED_RESPONSE_PRESETS_ENABLED);
        config.addDefault("suggested-response-presets",
                Arrays.asList("I see what you mean.", "That's interesting!", "Tell me more about that."));

        config.addDefault("web-search.enabled", DEFAULT_WEB_SEARCH_ENABLED);
        config.addDefault("web-search.auto-trigger", DEFAULT_WEB_SEARCH_AUTO_TRIGGER);
        config.addDefault("web-search.trigger-keywords", DEFAULT_WEB_SEARCH_TRIGGER_KEYWORDS);
        config.addDefault("web-search.result-count", DEFAULT_WEB_SEARCH_RESULT_COUNT);
        config.addDefault("web-search.engine", DEFAULT_WEB_SEARCH_ENGINE.getConfigName());
        config.addDefault("web-search.prompt-template", DEFAULT_WEB_SEARCH_PROMPT_TEMPLATE);

        config.addDefault("web-search.bocha.api-key", DEFAULT_BOCHA_API_KEY);
        config.addDefault("web-search.bocha.include-sites", DEFAULT_BOCHA_INCLUDE_SITES);
        config.addDefault("web-search.bocha.include-sites-list", DEFAULT_BOCHA_INCLUDE_SITES_LIST);
        config.addDefault("web-search.bocha.exclude-sites", DEFAULT_BOCHA_EXCLUDE_SITES);
        config.addDefault("web-search.bocha.exclude-sites-list", DEFAULT_BOCHA_EXCLUDE_SITES_LIST);
        config.addDefault("web-search.bocha.time-range", DEFAULT_BOCHA_TIME_RANGE);
        config.addDefault("web-search.bocha.freshness", DEFAULT_BOCHA_FRESHNESS);

        config.addDefault("web-search.brave.api-key", DEFAULT_BRAVE_API_KEY);
        config.addDefault("web-search.brave.country", DEFAULT_BRAVE_COUNTRY);
        config.addDefault("web-search.brave.search-lang", DEFAULT_BRAVE_SEARCH_LANG);
        config.addDefault("web-search.brave.ui-lang", DEFAULT_BRAVE_UI_LANG);
        config.addDefault("web-search.brave.safe-search", DEFAULT_BRAVE_SAFE_SEARCH);

        config.addDefault("database.type", "sqlite");
        config.addDefault("database.mysql.host", "localhost");
        config.addDefault("database.mysql.port", 3306);
        config.addDefault("database.mysql.database", "ollamachat");
        config.addDefault("database.mysql.username", "root");
        config.addDefault("database.mysql.password", "");

        config.addDefault("progress-display.enabled", true);
        config.addDefault("progress-display.type", "bossbar");
        config.addDefault("progress-display.color", "BLUE");
        config.addDefault("progress-display.style", "SOLID");
        config.addDefault("progress-display.update-interval", 1);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private void ensureConfigComplete() {
        FileConfiguration config = plugin.getConfig();
        boolean needsSave = false;

        if (!config.contains("ollama-api-url")) {
            config.set("ollama-api-url", DEFAULT_OLLAMA_API_URL);
            needsSave = true;
        }
        if (!config.contains("model")) {
            config.set("model", DEFAULT_OLLAMA_MODEL);
            needsSave = true;
        }
        if (!config.contains("language")) {
            config.set("language", "en_us");
            needsSave = true;
        }

        needsSave |= checkAndAddConfig(config, "web-search.enabled", DEFAULT_WEB_SEARCH_ENABLED);
        needsSave |= checkAndAddConfig(config, "web-search.auto-trigger", DEFAULT_WEB_SEARCH_AUTO_TRIGGER);
        needsSave |= checkAndAddConfig(config, "web-search.trigger-keywords", DEFAULT_WEB_SEARCH_TRIGGER_KEYWORDS);
        needsSave |= checkAndAddConfig(config, "web-search.result-count", DEFAULT_WEB_SEARCH_RESULT_COUNT);
        needsSave |= checkAndAddConfig(config, "web-search.engine", DEFAULT_WEB_SEARCH_ENGINE.getConfigName());
        needsSave |= checkAndAddConfig(config, "web-search.prompt-template", DEFAULT_WEB_SEARCH_PROMPT_TEMPLATE);

        needsSave |= checkAndAddConfig(config, "web-search.bocha.api-key", DEFAULT_BOCHA_API_KEY);
        needsSave |= checkAndAddConfig(config, "web-search.bocha.include-sites", DEFAULT_BOCHA_INCLUDE_SITES);
        needsSave |= checkAndAddConfig(config, "web-search.bocha.include-sites-list", DEFAULT_BOCHA_INCLUDE_SITES_LIST);
        needsSave |= checkAndAddConfig(config, "web-search.bocha.exclude-sites", DEFAULT_BOCHA_EXCLUDE_SITES);
        needsSave |= checkAndAddConfig(config, "web-search.bocha.exclude-sites-list", DEFAULT_BOCHA_EXCLUDE_SITES_LIST);
        needsSave |= checkAndAddConfig(config, "web-search.bocha.time-range", DEFAULT_BOCHA_TIME_RANGE);
        needsSave |= checkAndAddConfig(config, "web-search.bocha.freshness", DEFAULT_BOCHA_FRESHNESS);

        needsSave |= checkAndAddConfig(config, "web-search.brave.api-key", DEFAULT_BRAVE_API_KEY);
        needsSave |= checkAndAddConfig(config, "web-search.brave.country", DEFAULT_BRAVE_COUNTRY);
        needsSave |= checkAndAddConfig(config, "web-search.brave.search-lang", DEFAULT_BRAVE_SEARCH_LANG);
        needsSave |= checkAndAddConfig(config, "web-search.brave.ui-lang", DEFAULT_BRAVE_UI_LANG);
        needsSave |= checkAndAddConfig(config, "web-search.brave.safe-search", DEFAULT_BRAVE_SAFE_SEARCH);

        if (needsSave) {
            plugin.saveConfig();
            plugin.getLogger().info("Config file has been updated with missing settings.");
        }
    }

    private boolean checkAndAddConfig(FileConfiguration config, String path, Object defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            return true;
        }
        return false;
    }

    public void reloadConfigValues() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        ensureConfigComplete();

        config = plugin.getConfig();

        currentLanguage = config.getString("language", "en_us");

        ollamaApiUrl = config.getString("ollama-api-url", DEFAULT_OLLAMA_API_URL);
        ollamaModel = config.getString("model", DEFAULT_OLLAMA_MODEL);
        triggerPrefixes = config.getStringList("trigger-prefixes");
        if (triggerPrefixes.isEmpty()) {
            triggerPrefixes = DEFAULT_TRIGGER_PREFIXES;
        }

        maxResponseLength = config.getInt("max-response-length", DEFAULT_MAX_RESPONSE_LENGTH);
        ollamaEnabled = config.getBoolean("ollama-enabled", DEFAULT_OLLAMA_ENABLED);
        maxHistory = config.getInt("max-history", DEFAULT_MAX_HISTORY);
        streamingEnabled = config.getBoolean("stream-settings.enabled", DEFAULT_STREAMING_ENABLED);

        defaultPrompt = config.getString("default-prompt", "");
        loadPrompts(config);

        suggestedResponseModels = config.getStringList("suggested-response-models");
        if (suggestedResponseModels.isEmpty()) {
            suggestedResponseModels = Arrays.asList(DEFAULT_OLLAMA_MODEL);
        }

        suggestedResponsesEnabled = config.getBoolean("suggested-responses-enabled", DEFAULT_SUGGESTED_RESPONSES_ENABLED);
        suggestedResponseCount = config.getInt("suggested-response-count", DEFAULT_SUGGESTED_RESPONSE_COUNT);
        suggestedResponsePrompt = config.getString("suggested-response-prompt", getDefaultSuggestedPrompt());
        suggestedResponsePresets = config.getStringList("suggested-response-presets");
        suggestedResponseCooldown = config.getInt("suggested-response-cooldown", DEFAULT_SUGGESTED_RESPONSE_COOLDOWN);
        suggestedResponsePresetsEnabled = config.getBoolean("suggested-response-presets-enabled", DEFAULT_SUGGESTED_RESPONSE_PRESETS_ENABLED);

        webSearchEnabled = config.getBoolean("web-search.enabled", DEFAULT_WEB_SEARCH_ENABLED);
        webSearchAutoTrigger = config.getBoolean("web-search.auto-trigger", DEFAULT_WEB_SEARCH_AUTO_TRIGGER);
        webSearchTriggerKeywords = config.getStringList("web-search.trigger-keywords");
        if (webSearchTriggerKeywords.isEmpty()) {
            webSearchTriggerKeywords = DEFAULT_WEB_SEARCH_TRIGGER_KEYWORDS;
        }
        webSearchResultCount = config.getInt("web-search.result-count", DEFAULT_WEB_SEARCH_RESULT_COUNT);

        String engineName = config.getString("web-search.engine", DEFAULT_WEB_SEARCH_ENGINE.getConfigName());
        webSearchEngine = SearchEngine.fromString(engineName);

        webSearchPromptTemplate = config.getString("web-search.prompt-template", DEFAULT_WEB_SEARCH_PROMPT_TEMPLATE);

        bochaApiKey = config.getString("web-search.bocha.api-key", DEFAULT_BOCHA_API_KEY);
        bochaIncludeSites = config.getBoolean("web-search.bocha.include-sites", DEFAULT_BOCHA_INCLUDE_SITES);
        bochaIncludeSitesList = config.getStringList("web-search.bocha.include-sites-list");
        bochaExcludeSites = config.getBoolean("web-search.bocha.exclude-sites", DEFAULT_BOCHA_EXCLUDE_SITES);
        bochaExcludeSitesList = config.getStringList("web-search.bocha.exclude-sites-list");
        bochaTimeRange = config.getInt("web-search.bocha.time-range", DEFAULT_BOCHA_TIME_RANGE);
        bochaFreshness = config.getString("web-search.bocha.freshness", DEFAULT_BOCHA_FRESHNESS);

        braveApiKey = config.getString("web-search.brave.api-key", DEFAULT_BRAVE_API_KEY);
        braveCountry = config.getString("web-search.brave.country", DEFAULT_BRAVE_COUNTRY);
        braveSearchLang = config.getString("web-search.brave.search-lang", DEFAULT_BRAVE_SEARCH_LANG);
        braveUiLang = config.getString("web-search.brave.ui-lang", DEFAULT_BRAVE_UI_LANG);
        braveSafeSearch = config.getString("web-search.brave.safe-search", DEFAULT_BRAVE_SAFE_SEARCH);

        loadOtherAIConfigs(config);
        loadSuggestedResponseToggles(config);
    }

    /**
     */
    public void reloadLanguage() {
        String newLanguage = getCurrentLanguageFromConfig();
        if (!newLanguage.equals(currentLanguage) || langConfig == null) {
            currentLanguage = newLanguage;
            loadLanguageFile(currentLanguage);
            plugin.getLogger().info("Language changed to: " + currentLanguage);
        } else {
            loadLanguageFile(currentLanguage);
            plugin.getLogger().info("Language reloaded: " + currentLanguage);
        }
    }

    /**
     */
    private String getCurrentLanguageFromConfig() {
        return plugin.getConfig().getString("language", "en_us");
    }

    private void loadPrompts(FileConfiguration config) {
        prompts.clear();
        if (config.contains("prompts") && config.getConfigurationSection("prompts") != null) {
            for (String promptName : config.getConfigurationSection("prompts").getKeys(false)) {
                String promptContent = config.getString("prompts." + promptName);
                if (promptContent != null && !promptContent.isEmpty()) {
                    prompts.put(promptName, promptContent);
                }
            }
        }
    }

    private void loadOtherAIConfigs(FileConfiguration config) {
        otherAIConfigs = new HashMap<>();
        otherAIEnabled.clear();

        if (config.contains("other-ai-configs") && config.getConfigurationSection("other-ai-configs") != null) {
            for (String aiName : config.getConfigurationSection("other-ai-configs").getKeys(false)) {
                String path = "other-ai-configs." + aiName;
                String apiUrl = config.getString(path + ".api-url");
                String apiKey = config.getString(path + ".api-key", "");
                String model = config.getString(path + ".model");
                boolean enabled = config.getBoolean(path + ".enabled", true);
                boolean isMessagesFormat = config.getBoolean(path + ".messages-format", false);

                if (apiUrl != null && model != null) {
                    otherAIConfigs.put(aiName, new AIConfig(apiUrl, apiKey, model, isMessagesFormat));
                    otherAIEnabled.put(aiName, enabled);
                }
            }
        }
    }

    private void loadSuggestedResponseToggles(FileConfiguration config) {
        suggestedResponseModelToggles.clear();

        if (config.contains("suggested-response-model-toggles") &&
                config.getConfigurationSection("suggested-response-model-toggles") != null) {
            for (String model : config.getConfigurationSection("suggested-response-model-toggles").getKeys(false)) {
                boolean enabled = config.getBoolean("suggested-response-model-toggles." + model, true);
                suggestedResponseModelToggles.put(model, enabled);
            }
        }
    }

    private String getDefaultSuggestedPrompt() {
        return "Conversation:\nUser: {prompt}\nAI: {response}\n\n" +
                "Based on the above conversation, suggest {count} natural follow-up responses " +
                "the user might want to say. They should be conversational in tone rather than " +
                "questions. List them as:\n1. Response 1\n2. Response 2\n3. Response 3";
    }

    private void loadLanguageFile(String language) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveDefaultLanguageFiles();

        File langFile = new File(langFolder, language + ".json");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + language + ".json, falling back to en_us");
            langFile = new File(langFolder, "en_us.json");

            if (!langFile.exists()) {
                createEmptyLanguageFile(langFile);
            }
        }

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(langFile), StandardCharsets.UTF_8)) {
            langConfig = JsonParser.parseReader(reader).getAsJsonObject();
            if (langConfig == null) {
                langConfig = new JsonObject();
                plugin.getLogger().warning("Language file is empty or invalid: " + langFile.getName());
            } else {
                plugin.getLogger().info("Loaded language file: " + langFile.getName());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load language file: " + langFile.getName() + " - " + e.getMessage());
            langConfig = new JsonObject();
        }
    }

    private void saveDefaultLanguageFiles() {
        saveResourceIfNotExists("lang/en_us.json");
        saveResourceIfNotExists("lang/zh_cn.json");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File targetFile = new File(plugin.getDataFolder(), resourcePath);
        if (!targetFile.exists()) {
            // Use getResource to properly handle encoding from jar resources
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in != null) {
                    targetFile.getParentFile().mkdirs();
                    // Use UTF-8 encoding when writing language files
                    if (resourcePath.startsWith("lang/")) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(in, StandardCharsets.UTF_8));
                             BufferedWriter writer = new BufferedWriter(
                                     new OutputStreamWriter(
                                             new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                writer.write(line);
                                writer.newLine();
                            }
                        }
                    } else {
                        Files.copy(in, targetFile.toPath());
                    }
                    plugin.getLogger().info("Created default file: " + resourcePath);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create default file: " + resourcePath);
            }
        }
    }

    private void createEmptyLanguageFile(File langFile) {
        try {
            langFile.getParentFile().mkdirs();
            langFile.createNewFile();
            JsonObject emptyJson = new JsonObject();
            // Write with UTF-8 encoding
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(langFile), StandardCharsets.UTF_8)) {
                writer.write(gson.toJson(emptyJson));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create empty language file: " + e.getMessage());
        }
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        if (langConfig == null || !langConfig.has(key)) {
            return "§c[OllamaChat] Missing language key: " + key;
        }

        String message = langConfig.get(key).getAsString();

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public void setDefaultPrompt(String prompt) {
        this.defaultPrompt = prompt;
        plugin.getConfig().set("default-prompt", prompt);
        plugin.saveConfig();
    }

    public void addPrompt(String name, String content) {
        prompts.put(name, content);
        plugin.getConfig().set("prompts." + name, content);
        plugin.saveConfig();
    }

    public void removePrompt(String name) {
        prompts.remove(name);
        plugin.getConfig().set("prompts." + name, null);

        if (defaultPrompt != null && defaultPrompt.equals(name)) {
            setDefaultPrompt("");
        }

        plugin.saveConfig();
    }

    public void setOllamaEnabled(boolean enabled) {
        this.ollamaEnabled = enabled;
        plugin.getConfig().set("ollama-enabled", enabled);
        plugin.saveConfig();
    }

    public void setSuggestedResponsePresetsEnabled(boolean enabled) {
        this.suggestedResponsePresetsEnabled = enabled;
        plugin.getConfig().set("suggested-response-presets-enabled", enabled);
        plugin.saveConfig();
    }

    public void setSuggestedResponseModelToggle(String model, boolean enabled) {
        suggestedResponseModelToggles.put(model, enabled);
        plugin.getConfig().set("suggested-response-model-toggles." + model, enabled);
        plugin.saveConfig();
    }

    public void setWebSearchEnabled(boolean enabled) {
        this.webSearchEnabled = enabled;
        plugin.getConfig().set("web-search.enabled", enabled);
        plugin.saveConfig();
    }

    public void setWebSearchEngine(SearchEngine engine) {
        this.webSearchEngine = engine;
        plugin.getConfig().set("web-search.engine", engine.getConfigName());
        plugin.saveConfig();
    }

    public void setWebSearchAutoTrigger(boolean autoTrigger) {
        this.webSearchAutoTrigger = autoTrigger;
        plugin.getConfig().set("web-search.auto-trigger", autoTrigger);
        plugin.saveConfig();
    }

    public void setWebSearchTriggerKeywords(List<String> keywords) {
        this.webSearchTriggerKeywords = keywords;
        plugin.getConfig().set("web-search.trigger-keywords", keywords);
        plugin.saveConfig();
    }

    public void setWebSearchResultCount(int count) {
        this.webSearchResultCount = count;
        plugin.getConfig().set("web-search.result-count", count);
        plugin.saveConfig();
    }

    // Bocha Setter
    public void setBochaApiKey(String apiKey) {
        this.bochaApiKey = apiKey;
        plugin.getConfig().set("web-search.bocha.api-key", apiKey);
        plugin.saveConfig();
    }

    public void setBochaIncludeSites(boolean include) {
        this.bochaIncludeSites = include;
        plugin.getConfig().set("web-search.bocha.include-sites", include);
        plugin.saveConfig();
    }

    public void setBochaIncludeSitesList(List<String> sites) {
        this.bochaIncludeSitesList = sites;
        plugin.getConfig().set("web-search.bocha.include-sites-list", sites);
        plugin.saveConfig();
    }

    public void setBochaExcludeSites(boolean exclude) {
        this.bochaExcludeSites = exclude;
        plugin.getConfig().set("web-search.bocha.exclude-sites", exclude);
        plugin.saveConfig();
    }

    public void setBochaExcludeSitesList(List<String> sites) {
        this.bochaExcludeSitesList = sites;
        plugin.getConfig().set("web-search.bocha.exclude-sites-list", sites);
        plugin.saveConfig();
    }

    public void setBochaTimeRange(int days) {
        this.bochaTimeRange = days;
        plugin.getConfig().set("web-search.bocha.time-range", days);
        plugin.saveConfig();
    }

    public void setBochaFreshness(String freshness) {
        this.bochaFreshness = freshness;
        plugin.getConfig().set("web-search.bocha.freshness", freshness);
        plugin.saveConfig();
    }

    // Brave Setter
    public void setBraveApiKey(String apiKey) {
        this.braveApiKey = apiKey;
        plugin.getConfig().set("web-search.brave.api-key", apiKey);
        plugin.saveConfig();
    }

    public void setBraveCountry(String country) {
        this.braveCountry = country;
        plugin.getConfig().set("web-search.brave.country", country);
        plugin.saveConfig();
    }

    public void setBraveSearchLang(String lang) {
        this.braveSearchLang = lang;
        plugin.getConfig().set("web-search.brave.search-lang", lang);
        plugin.saveConfig();
    }

    public void setBraveUiLang(String lang) {
        this.braveUiLang = lang;
        plugin.getConfig().set("web-search.brave.ui-lang", lang);
        plugin.saveConfig();
    }

    public void setBraveSafeSearch(String safeSearch) {
        this.braveSafeSearch = safeSearch;
        plugin.getConfig().set("web-search.brave.safe-search", safeSearch);
        plugin.saveConfig();
    }

    public void setWebSearchPromptTemplate(String template) {
        this.webSearchPromptTemplate = template;
        plugin.getConfig().set("web-search.prompt-template", template);
        plugin.saveConfig();
    }

    // Getters
    public boolean isWebSearchEnabled() { return webSearchEnabled; }
    public boolean isWebSearchAutoTrigger() { return webSearchAutoTrigger; }
    public List<String> getWebSearchTriggerKeywords() { return webSearchTriggerKeywords; }
    public int getWebSearchResultCount() { return webSearchResultCount; }
    public SearchEngine getWebSearchEngine() { return webSearchEngine; }
    public String getWebSearchPromptTemplate() { return webSearchPromptTemplate; }

    public String getBochaApiKey() { return bochaApiKey; }
    public boolean isBochaIncludeSites() { return bochaIncludeSites; }
    public List<String> getBochaIncludeSites() { return bochaIncludeSitesList; }
    public boolean isBochaExcludeSites() { return bochaExcludeSites; }
    public List<String> getBochaExcludeSites() { return bochaExcludeSitesList; }
    public int getBochaTimeRange() { return bochaTimeRange; }
    public String getBochaFreshness() { return bochaFreshness; }

    public String getBraveApiKey() { return braveApiKey; }
    public String getBraveCountry() { return braveCountry; }
    public String getBraveSearchLang() { return braveSearchLang; }
    public String getBraveUiLang() { return braveUiLang; }
    public String getBraveSafeSearch() { return braveSafeSearch; }

    public String getCurrentLanguage() { return currentLanguage; }
    public String getOllamaApiUrl() { return ollamaApiUrl; }
    public String getOllamaModel() { return ollamaModel; }
    public List<String> getTriggerPrefixes() { return triggerPrefixes; }
    public int getMaxResponseLength() { return maxResponseLength; }
    public Map<String, AIConfig> getOtherAIConfigs() { return otherAIConfigs; }
    public boolean isOllamaEnabled() { return ollamaEnabled; }
    public Map<String, Boolean> getOtherAIEnabled() { return otherAIEnabled; }
    public boolean isStreamingEnabled() { return streamingEnabled; }
    public String getDefaultPrompt() { return defaultPrompt; }
    public Map<String, String> getPrompts() { return prompts; }
    public Map<UUID, Map<String, String>> getSelectedConversations() { return selectedConversations; }
    public int getMaxHistory() { return maxHistory; }
    public List<String> getSuggestedResponseModels() { return suggestedResponseModels; }
    public boolean isSuggestedResponsesEnabled() { return suggestedResponsesEnabled; }
    public int getSuggestedResponseCount() { return suggestedResponseCount; }
    public String getSuggestedResponsePrompt() { return suggestedResponsePrompt; }
    public List<String> getSuggestedResponsePresets() { return suggestedResponsePresets; }
    public Map<String, Boolean> getSuggestedResponseModelToggles() { return suggestedResponseModelToggles; }
    public int getSuggestedResponseCooldown() { return suggestedResponseCooldown; }
    public boolean isSuggestedResponsePresetsEnabled() { return suggestedResponsePresetsEnabled; }

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

        public String getApiUrl() { return apiUrl; }
        public String getApiKey() { return apiKey; }
        public String getModel() { return model; }
        public boolean isMessagesFormat() { return isMessagesFormat; }
    }
}