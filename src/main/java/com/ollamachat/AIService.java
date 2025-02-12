package com.ollamachat;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AIService {

    private final HttpClient httpClient;
    private final Gson gson;

    public AIService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public CompletableFuture<String> sendRequest(String apiUrl, String apiKey, String model, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "prompt", prompt,
                        "stream", false
                );

                String jsonRequest = gson.toJson(requestBody);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonRequest));

                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + apiKey);
                }

                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    throw new RuntimeException("AI API Error: " + response.body());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to get response from AI: " + e.getMessage(), e);
            }
        });
    }
}
