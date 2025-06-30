package com.ollamachat.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ollamachat.AIService;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SuggestedResponseHandler {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final AIService aiService;
    private final Gson gson;

    public SuggestedResponseHandler(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.aiService = new AIService();
        this.gson = new Gson();
    }

    public void sendSuggestedResponses(Player player, String originalAIName, String originalPrompt, String originalResponse) {
        if (!configManager.isSuggestedResponsesEnabled()) return;

        List<String> suggestedModels = configManager.getSuggestedResponseModels();
        if (suggestedModels.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            List<String> suggestedResponses = new ArrayList<>();
            for (String model : suggestedModels) {
                if (model.equalsIgnoreCase(originalAIName)) continue; // Skip the original model
                try {
                    String apiUrl;
                    String apiKey;
                    boolean isMessagesFormat;
                    // Check if the model is in otherAIConfigs
                    ConfigManager.AIConfig aiConfig = configManager.getOtherAIConfigs().get(model);
                    if (aiConfig != null && configManager.getOtherAIEnabled().getOrDefault(model, false)) {
                        apiUrl = aiConfig.getApiUrl();
                        apiKey = aiConfig.getApiKey();
                        isMessagesFormat = aiConfig.isMessagesFormat();
                    } else {
                        // Fall back to Ollama configuration
                        apiUrl = configManager.getOllamaApiUrl();
                        apiKey = null;
                        isMessagesFormat = false;
                    }
                    // Craft a prompt to generate suggested user questions
                    String context = "Conversation:\nUser: " + originalPrompt + "\nAI: " + originalResponse + "\n\n" +
                            "Based on the above conversation, suggest 3 follow-up questions or prompts the user might want to ask. " +
                            "List them as:\n1. Question 1\n2. Question 2\n3. Question 3";
                    String responseBody = aiService.sendRequest(apiUrl, apiKey, model, context, isMessagesFormat).join();
                    String suggestedText;
                    if (isMessagesFormat) {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        suggestedText = json.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .get("message").getAsJsonObject()
                                .get("content").getAsString();
                    } else {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        suggestedText = json.get("response").getAsString();
                    }
                    // Split the response into individual suggestions
                    String[] suggestions = suggestedText.split("\n");
                    for (String suggestion : suggestions) {
                        // Clean up numbering (e.g., remove "1. " prefix)
                        String cleanedSuggestion = suggestion.replaceAll("^\\d+\\.\\s*", "").trim();
                        if (!cleanedSuggestion.isEmpty()) {
                            suggestedResponses.add("[" + model + "] " + cleanedSuggestion);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to get suggested response from " + model + ": " + e.getMessage());
                }
            }

            if (player.isOnline() && !suggestedResponses.isEmpty()) {
                // Send header
                player.sendMessage(Component.text(configManager.getMessage("suggested-responses-header", null), NamedTextColor.GREEN));
                // Send each suggested response as a clickable message
                for (String response : suggestedResponses) {
                    if (response.length() > configManager.getMaxResponseLength()) {
                        response = response.substring(0, configManager.getMaxResponseLength()) + "...";
                        player.sendMessage(Component.text(configManager.getMessage("response-truncated", null), NamedTextColor.YELLOW));
                    }
                    String prefix = configManager.getMessage("suggested-response-prefix", null);
                    String command = "/aichat " + originalAIName + " " + response.replace("\"", "\\\"");
                    Component message = Component.text(prefix + response)
                            .color(NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand(command));
                    player.sendMessage(message);
                }
            }
        });
    }
}







