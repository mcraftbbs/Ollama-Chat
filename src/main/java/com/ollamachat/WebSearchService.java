package com.ollamachat;

import com.google.gson.Gson;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WebSearchService {
    private final Ollamachat plugin;
    private final ConfigManager configManager;

    private final BochaSearchService bochaService;
    private final BraveSearchService braveService;

    public WebSearchService(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();

        this.bochaService = new BochaSearchService(plugin);
        this.braveService = new BraveSearchService(plugin);
    }

    public CompletableFuture<List<SearchResult>> search(String query, int count) {
        ConfigManager.SearchEngine engine = configManager.getWebSearchEngine();

        if (!isSearchEngineConfigured(engine)) {
            plugin.getLogger().warning("Search engine " + engine.getConfigName() + " is not properly configured");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        switch (engine) {
            case BOCHA:
                return bochaService.search(query, count);
            case BRAVE:
                return braveService.search(query, count);
            default:
                return braveService.search(query, count);
        }
    }

    private boolean isSearchEngineConfigured(ConfigManager.SearchEngine engine) {
        switch (engine) {
            case BOCHA:
                String bochaKey = configManager.getBochaApiKey();
                return bochaKey != null && !bochaKey.isEmpty();
            case BRAVE:
                String braveKey = configManager.getBraveApiKey();
                return braveKey != null && !braveKey.isEmpty();
            default:
                return false;
        }
    }

    public String getCurrentEngineName() {
        return configManager.getWebSearchEngine().getConfigName();
    }

    public String getCurrentEngineDisplayName() {
        ConfigManager.SearchEngine engine = configManager.getWebSearchEngine();
        switch (engine) {
            case BOCHA:
                return "Bocha";
            case BRAVE:
                return "Brave Search";
            default:
                return engine.getConfigName();
        }
    }

    public boolean engineRequiresApiKey(ConfigManager.SearchEngine engine) {
        return engine == ConfigManager.SearchEngine.BOCHA ||
                engine == ConfigManager.SearchEngine.BRAVE;
    }

    public String formatSearchResultsForAI(List<SearchResult> results, String query) {
        if (results == null || results.isEmpty()) {
            return configManager.getMessage("websearch-no-results",
                    query != null ? Map.of("query", query) : null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(configManager.getMessage("websearch-results-header",
                query != null ? Map.of("query", query) : null)).append("\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            sb.append(i + 1).append(". **").append(result.getTitle()).append("**\n");
            sb.append("   ").append(configManager.getMessage("websearch-source",
                    Map.of("site", result.getSiteName() != null ? result.getSiteName() : "Unknown"))).append("\n");
            sb.append("   ").append(result.getSnippet()).append("\n");
            sb.append("   ").append(configManager.getMessage("websearch-url",
                    Map.of("url", result.getUrl() != null ? result.getUrl() : "#"))).append("\n\n");
        }

        return sb.toString();
    }

    public static class SearchResult {
        private String title;
        private String url;
        private String snippet;
        private String siteName;
        private String dateLastCrawled;

        public String getTitle() { return title != null ? title : ""; }
        public void setTitle(String title) { this.title = title; }

        public String getUrl() { return url != null ? url : ""; }
        public void setUrl(String url) { this.url = url; }

        public String getSnippet() { return snippet != null ? snippet : ""; }
        public void setSnippet(String snippet) { this.snippet = snippet; }

        public String getSiteName() { return siteName != null ? siteName : ""; }
        public void setSiteName(String siteName) { this.siteName = siteName; }

        public String getDateLastCrawled() { return dateLastCrawled != null ? dateLastCrawled : ""; }
        public void setDateLastCrawled(String dateLastCrawled) { this.dateLastCrawled = dateLastCrawled; }

        @Override
        public String toString() {
            return "SearchResult{" +
                    "title='" + title + '\'' +
                    ", url='" + url + '\'' +
                    ", siteName='" + siteName + '\'' +
                    '}';
        }
    }
}