package com.ollamachat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Consumer;
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

    public CompletableFuture<Void> sendStreamingRequest(String apiUrl, String apiKey, String model, String prompt, Consumer<String> responseConsumer) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "prompt", prompt,
                        "stream", true
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
                    StringBuilder buffer = new StringBuilder();
                    int minBufferLength = 50; // Minimum length before sending
                    String[] lines = response.body().split("\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            JsonObject json = gson.fromJson(line, JsonObject.class);
                            if (json.has("response")) {
                                String partialResponse = json.get("response").getAsString();
                                buffer.append(partialResponse);

                                // Check if buffer ends with a sentence boundary or is long enough
                                String currentBuffer = buffer.toString();
                                if (currentBuffer.endsWith(".") || currentBuffer.endsWith("?") ||
                                        currentBuffer.endsWith("!") || currentBuffer.length() >= minBufferLength) {
                                    responseConsumer.accept(currentBuffer);
                                    buffer.setLength(0); // Clear buffer
                                }
                            }
                        }
                    }
                    // Send any remaining content in the buffer
                    if (buffer.length() > 0) {
                        responseConsumer.accept(buffer.toString());
                    }
                } else {
                    throw new RuntimeException("AI API Error: " + response.body());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to get streaming response from AI: " + e.getMessage(), e);
            }
        });
    }
}

