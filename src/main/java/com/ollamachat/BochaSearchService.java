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

public class BochaSearchService {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final HttpClient httpClient;
    private final Gson gson;

    private static final String BOCHA_API_URL = "https://api.bocha.cn/v1/web-search";

    public BochaSearchService(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     */
    public CompletableFuture<List<WebSearchService.SearchResult>> search(String query, int count) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = configManager.getBochaApiKey();
                if (apiKey == null || apiKey.isEmpty()) {
                    plugin.getLogger().warning("Bocha API key is not configured");
                    return Collections.emptyList();
                }

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("query", query);
                requestBody.put("count", count);

                if (configManager.isBochaIncludeSites() && !configManager.getBochaIncludeSites().isEmpty()) {
                    requestBody.put("includeSites", configManager.getBochaIncludeSites());
                }
                if (configManager.isBochaExcludeSites() && !configManager.getBochaExcludeSites().isEmpty()) {
                    requestBody.put("excludeSites", configManager.getBochaExcludeSites());
                }
                if (configManager.getBochaTimeRange() > 0) {
                    requestBody.put("timeRange", configManager.getBochaTimeRange());
                }
                if (configManager.getBochaFreshness() != null && !configManager.getBochaFreshness().isEmpty()) {
                    requestBody.put("freshness", configManager.getBochaFreshness());
                }

                String jsonRequest = gson.toJson(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BOCHA_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseSearchResults(response.body());
                } else {
                    plugin.getLogger().warning("Bocha API error: " + response.statusCode() + " - " + response.body());
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to perform Bocha search: " + e.getMessage());
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    /**
     */
    private List<WebSearchService.SearchResult> parseSearchResults(String jsonResponse) {
        List<WebSearchService.SearchResult> results = new ArrayList<>();

        try {
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

            if (!root.has("code") || root.get("code").getAsInt() != 200) {
                String msg = root.has("msg") ? root.get("msg").getAsString() : "Unknown error";
                plugin.getLogger().warning("Bocha API returned error: " + msg);
                return results;
            }

            if (!root.has("data")) {
                return results;
            }

            JsonObject data = root.getAsJsonObject("data");
            if (!data.has("webPages")) {
                return results;
            }

            JsonObject webPages = data.getAsJsonObject("webPages");
            if (!webPages.has("value")) {
                return results;
            }

            JsonArray values = webPages.getAsJsonArray("value");

            for (int i = 0; i < values.size(); i++) {
                JsonObject item = values.get(i).getAsJsonObject();

                WebSearchService.SearchResult result = new WebSearchService.SearchResult();
                result.setTitle(getJsonString(item, "name"));
                result.setUrl(getJsonString(item, "url"));
                result.setSnippet(getJsonString(item, "snippet"));
                result.setSiteName(getJsonString(item, "siteName"));
                result.setDateLastCrawled(getJsonString(item, "dateLastCrawled"));

                results.add(result);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse Bocha search results: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private String getJsonString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
