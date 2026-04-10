package com.ollamachat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BraveSearchService {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final HttpClient httpClient;
    private final Gson gson;

    private static final String BRAVE_API_URL = "https://api.search.brave.com/res/v1/web/search";

    public BraveSearchService(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public CompletableFuture<List<WebSearchService.SearchResult>> search(String query, int count) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = configManager.getBraveApiKey();
                if (apiKey == null || apiKey.isEmpty()) {
                    plugin.getLogger().warning("Brave API key is not configured");
                    return Collections.emptyList();
                }

                // Build query parameters
                StringBuilder urlBuilder = new StringBuilder(BRAVE_API_URL);
                urlBuilder.append("?q=").append(query.replace(" ", "+"));
                urlBuilder.append("&count=").append(count);
                urlBuilder.append("&country=").append(configManager.getBraveCountry());
                urlBuilder.append("&search_lang=").append(configManager.getBraveSearchLang());
                urlBuilder.append("&ui_lang=").append(configManager.getBraveUiLang());
                urlBuilder.append("&safe_search=").append(configManager.getBraveSafeSearch());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlBuilder.toString()))
                        .header("Accept", "application/json")
                        .header("Accept-Encoding", "gzip")
                        .header("X-Subscription-Token", apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseSearchResults(response.body());
                } else {
                    plugin.getLogger().warning("Brave API error: " + response.statusCode() + " - " + response.body());
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to perform Brave search: " + e.getMessage());
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    private List<WebSearchService.SearchResult> parseSearchResults(String jsonResponse) {
        List<WebSearchService.SearchResult> results = new ArrayList<>();

        try {
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

            if (!root.has("web") || !root.getAsJsonObject("web").has("results")) {
                return results;
            }

            JsonArray resultsArray = root.getAsJsonObject("web").getAsJsonArray("results");

            for (int i = 0; i < resultsArray.size(); i++) {
                JsonObject item = resultsArray.get(i).getAsJsonObject();

                WebSearchService.SearchResult result = new WebSearchService.SearchResult();
                result.setTitle(getJsonString(item, "title"));
                result.setUrl(getJsonString(item, "url"));
                result.setSnippet(getJsonString(item, "description"));
                result.setSiteName(extractDomain(getJsonString(item, "url")));

                if (item.has("age") && !item.get("age").isJsonNull()) {
                    result.setDateLastCrawled(item.get("age").getAsString());
                }

                results.add(result);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse Brave search results: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private String extractDomain(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String host = urlObj.getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return url;
        }
    }
}