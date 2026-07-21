package com.ollamachat.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ollamachat.AIService;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;
import com.ollamachat.format.MarkdownFormatter;
import com.ollamachat.scheduler.SchedulerUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SuggestedResponseHandler {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final AIService aiService;
    private final Gson gson;
    private final Map<UUID, Long> lastSuggestionTimes;
    private final MarkdownFormatter markdownFormatter;
    private final ClickableMessageSender clickableSender;

    public SuggestedResponseHandler(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.aiService = new AIService();
        this.gson = new Gson();
        this.lastSuggestionTimes = new HashMap<>();
        this.markdownFormatter = configManager.getMarkdownFormatter();
        this.clickableSender = createClickableSender();
    }

    private ClickableMessageSender createClickableSender() {
        try {
            Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Class.forName("net.md_5.bungee.api.chat.ClickEvent");
            return new SpigotClickableSender();
        } catch (ClassNotFoundException e) {
            return new BukkitClickableSender();
        }
    }

    public boolean isSuggestionsEnabledForPlayer(Player player) {
        Map<UUID, Boolean> playerSuggestionToggles = plugin.getPlayerSuggestionToggles();
        return playerSuggestionToggles.computeIfAbsent(player.getUniqueId(), k -> true);
    }

    public void toggleSuggestionsForPlayer(Player player, boolean enabled) {
        plugin.getPlayerSuggestionToggles().put(player.getUniqueId(), enabled);
    }

    public boolean canGenerateSuggestions(Player player) {
        int cooldown = configManager.getSuggestedResponseCooldown();
        if (cooldown <= 0) return true;

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastSuggestionTimes.get(player.getUniqueId());
        if (lastTime == null || (currentTime - lastTime) / 1000 >= cooldown) {
            lastSuggestionTimes.put(player.getUniqueId(), currentTime);
            return true;
        }
        return false;
    }

    public void sendSuggestedResponses(Player player, String originalAIName,
                                       String originalPrompt, String originalResponse) {
        if (!configManager.isSuggestedResponsesEnabled() || !isSuggestionsEnabledForPlayer(player)) {
            return;
        }

        if (!canGenerateSuggestions(player)) {
            int cooldown = configManager.getSuggestedResponseCooldown();
            long lastTime = lastSuggestionTimes.get(player.getUniqueId());
            long secondsRemaining = cooldown - (System.currentTimeMillis() - lastTime) / 1000;
            sendMessage(player, ChatColor.RED + configManager.getMessage("suggests-rate-limit",
                    Map.of("seconds", String.valueOf(secondsRemaining))));
            return;
        }

        List<String> suggestedResponses = new ArrayList<>();

        // Add preset responses if enabled
        if (configManager.isSuggestedResponsePresetsEnabled()) {
            suggestedResponses.addAll(configManager.getSuggestedResponsePresets());
        }

        // Generate AI-suggested responses
        List<String> suggestedModels = configManager.getSuggestedResponseModels();
        if (!suggestedModels.isEmpty()) {
            // Use SchedulerUtils for Folia compatibility
            SchedulerUtils.runAsync(plugin, () -> {
                for (String model : suggestedModels) {
                    if (model.equalsIgnoreCase(originalAIName)) continue;
                    if (!configManager.getSuggestedResponseModelToggles().getOrDefault(model, true)) continue;

                    try {
                        String apiUrl;
                        String apiKey;
                        boolean isMessagesFormat;

                        // Determine which API to use for this model
                        ConfigManager.AIConfig aiConfig = configManager.getOtherAIConfigs().get(model);
                        if (aiConfig != null && configManager.getOtherAIEnabled().getOrDefault(model, false)) {
                            apiUrl = aiConfig.getApiUrl();
                            apiKey = aiConfig.getApiKey();
                            isMessagesFormat = aiConfig.isMessagesFormat();
                        } else {
                            apiUrl = configManager.getOllamaApiUrl();
                            apiKey = null;
                            isMessagesFormat = false;
                        }

                        // Build the suggestion prompt
                        String promptTemplate = configManager.getSuggestedResponsePrompt();
                        String context = promptTemplate
                                .replace("{prompt}", originalPrompt)
                                .replace("{response}", originalResponse)
                                .replace("{count}", String.valueOf(configManager.getSuggestedResponseCount()));

                        // Get suggestions from AI
                        String responseBody = aiService.sendRequest(
                                apiUrl, apiKey, model, context, isMessagesFormat
                        ).join();

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

                        // Parse individual suggestions
                        String[] suggestions = suggestedText.split("\n");
                        for (String suggestion : suggestions) {
                            String cleanedSuggestion = suggestion.replaceAll("^\\d+\\.\\s*", "").trim();
                            if (!cleanedSuggestion.isEmpty()) {
                                // Apply Markdown formatting to clean the suggestion
                                cleanedSuggestion = markdownFormatter.format(cleanedSuggestion);
                                suggestedResponses.add("[" + model + "] " + cleanedSuggestion);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning(
                                "Failed to get suggested response from " + model + ": " + e.getMessage()
                        );
                    }
                }

                // Send suggestions to player on the server thread
                final List<String> finalResponses = new ArrayList<>(suggestedResponses);
                SchedulerUtils.runOnEntity(plugin, player, () -> {
                    if (player.isOnline() && !finalResponses.isEmpty()) {
                        // Send header
                        sendMessage(player, ChatColor.GREEN +
                                configManager.getMessage("suggested-responses-header", null));

                        // Send each suggestion
                        for (int i = 0; i < Math.min(finalResponses.size(),
                                configManager.getSuggestedResponseCount()); i++) {
                            String response = finalResponses.get(i);

                            // Truncate if too long
                            if (response.length() > configManager.getMaxResponseLength()) {
                                response = response.substring(0, configManager.getMaxResponseLength()) + "...";
                                sendMessage(player, ChatColor.YELLOW +
                                        configManager.getMessage("response-truncated", null));
                            }

                            // Create clickable message component
                            String displayText = configManager.getMessage("suggested-response-prefix", null) + response;

                            // Clean response for command (escape special characters)
                            String cleanResponse = response.replace("\\", "\\\\").replace("\"", "\\\"");
                            String command = "/aichat " + originalAIName + " " + cleanResponse;
                            String hoverText = configManager.getMessage("suggested-response-hover", null);

                            sendClickableMessage(player, displayText, command, hoverText);
                        }
                    }
                });
            });
        } else if (player.isOnline() && !suggestedResponses.isEmpty()) {
            // Send only preset suggestions
            sendMessage(player, ChatColor.GREEN +
                    configManager.getMessage("suggested-responses-header", null));

            for (String response : suggestedResponses) {
                if (response.length() > configManager.getMaxResponseLength()) {
                    response = response.substring(0, configManager.getMaxResponseLength()) + "...";
                    sendMessage(player, ChatColor.YELLOW +
                            configManager.getMessage("response-truncated", null));
                }

                String displayText = configManager.getMessage("suggested-response-prefix", null) + response;
                String cleanResponse = response.replace("\\", "\\\\").replace("\"", "\\\"");
                String command = "/aichat " + originalAIName + " " + cleanResponse;
                String hoverText = configManager.getMessage("suggested-response-hover", null);

                sendClickableMessage(player, displayText, command, hoverText);
            }
        }
    }

    private void sendMessage(Player player, String message) {
        if (player.isOnline()) {
            player.sendMessage(message);
        }
    }

    private void sendClickableMessage(Player player, String text, String command, String hoverText) {
        clickableSender.sendClickableMessage(player, text, command, hoverText);
    }
}
