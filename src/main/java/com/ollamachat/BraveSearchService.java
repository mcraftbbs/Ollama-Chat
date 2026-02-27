package com.ollamachat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Service for accessing the Brave Search API
 */
public class BraveSearchService {

    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private static final String API_URL = "https://api.search.brave.com/res/v1/web/search";
    private static final int MAX_RESULTS = 5;

    public BraveSearchService(String apiKey) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.apiKey = apiKey;
    }

    /**
     * Searches the web using Brave Search API
     * @param query The search query
     * @return CompletableFuture containing formatted search results
     */
    public CompletableFuture<String> search(String query) {
        return CompletableFuture.supplyAsync(() -> {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("Brave Search API key is not configured");
            }

            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = API_URL + "?q=" + encodedQuery + "&count=" + MAX_RESULTS;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("X-Subscription-Token", apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    return parseSearchResults(response.body());
                } else {
                    throw new RuntimeException("Brave Search API Error: " + response.statusCode() + " - " + response.body());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to search with Brave Search: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Parses search results from Brave Search API response
     * @param responseBody The JSON response from Brave Search API
     * @return Formatted string containing search results
     */
    private String parseSearchResults(String responseBody) {
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            if (response == null) {
                throw new RuntimeException("Empty response from Brave Search API");
            }
            JsonObject web = response.getAsJsonObject("web");
            JsonArray results = web != null ? web.getAsJsonArray("results") : null;

            if (results == null || results.size() == 0) {
                throw new RuntimeException("No search results found in Brave Search API response");
            }

            StringBuilder sb = new StringBuilder("Search Results:\n");
            for (int i = 0; i < Math.min(results.size(), MAX_RESULTS); i++) {
                JsonObject result = results.get(i).getAsJsonObject();
                String title = result.has("title") ? result.get("title").getAsString() : "No Title";
                String description = result.has("description") ? result.get("description").getAsString() : "No Description";
                String url = result.has("url") ? result.get("url").getAsString() : "";

                sb.append(i + 1).append(". ").append(title).append("\n");
                sb.append("   Description: ").append(truncate(description, 150)).append("\n");
                if (!url.isEmpty()) {
                    sb.append("   URL: ").append(url).append("\n");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse search results: " + e.getMessage(), e);
        }
    }

    /**
     * Truncates text to a maximum length
     * @param text The text to truncate
     * @param maxLength Maximum length
     * @return Truncated text
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
