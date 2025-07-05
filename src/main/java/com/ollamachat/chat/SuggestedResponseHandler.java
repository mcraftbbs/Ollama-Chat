package com.ollamachat.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ollamachat.AIService;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SuggestedResponseHandler {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final AIService aiService;
    private final Gson gson;
    private final Map<UUID, Long> lastSuggestionTimes;

    public SuggestedResponseHandler(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.aiService = new AIService();
        this.gson = new Gson();
        this.lastSuggestionTimes = new HashMap<>();
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

    public void sendSuggestedResponses(Player player, String originalAIName, String originalPrompt, String originalResponse) {
        if (!configManager.isSuggestedResponsesEnabled() || !isSuggestionsEnabledForPlayer(player)) return;
        if (!canGenerateSuggestions(player)) {
            int cooldown = configManager.getSuggestedResponseCooldown();
            long lastTime = lastSuggestionTimes.get(player.getUniqueId());
            long secondsRemaining = cooldown - (System.currentTimeMillis() - lastTime) / 1000;
            player.sendMessage(Component.text(configManager.getMessage("suggests-rate-limit", Map.of("seconds", String.valueOf(secondsRemaining))), NamedTextColor.RED));
            return;
        }

        List<String> suggestedResponses = new ArrayList<>();
        if (configManager.isSuggestedResponsePresetsEnabled()) {
            suggestedResponses.addAll(configManager.getSuggestedResponsePresets());
        }

        List<String> suggestedModels = configManager.getSuggestedResponseModels();
        if (!suggestedModels.isEmpty()) {
            CompletableFuture.runAsync(() -> {
                for (String model : suggestedModels) {
                    if (model.equalsIgnoreCase(originalAIName)) continue;
                    if (!configManager.getSuggestedResponseModelToggles().getOrDefault(model, true)) continue;
                    try {
                        String apiUrl;
                        String apiKey;
                        boolean isMessagesFormat;
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
                        String promptTemplate = configManager.getSuggestedResponsePrompt();
                        String context = promptTemplate
                                .replace("{prompt}", originalPrompt)
                                .replace("{response}", originalResponse)
                                .replace("{count}", String.valueOf(configManager.getSuggestedResponseCount()));
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
                        String[] suggestions = suggestedText.split("\n");
                        for (String suggestion : suggestions) {
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
                    player.sendMessage(Component.text(configManager.getMessage("suggested-responses-header", null), NamedTextColor.GREEN));
                    for (String response : suggestedResponses) {
                        if (response.length() > configManager.getMaxResponseLength()) {
                            response = response.substring(0, configManager.getMaxResponseLength()) + "...";
                            player.sendMessage(Component.text(configManager.getMessage("response-truncated", null), NamedTextColor.YELLOW));
                        }
                        String prefix = configManager.getMessage("suggested-response-prefix", null);
                        String command = "/aichat " + originalAIName + " " + response.replace("\"", "\\\"");
                        Component message = Component.text(prefix + response)
                                .color(NamedTextColor.WHITE)
                                .clickEvent(ClickEvent.runCommand(command))
                                .hoverEvent(HoverEvent.showText(Component.text(configManager.getMessage("suggested-response-hover", null), NamedTextColor.YELLOW)));
                        player.sendMessage(message);
                    }
                }
            });
        } else if (player.isOnline() && !suggestedResponses.isEmpty()) {
            player.sendMessage(Component.text(configManager.getMessage("suggested-responses-header", null), NamedTextColor.GREEN));
            for (String response : suggestedResponses) {
                if (response.length() > configManager.getMaxResponseLength()) {
                    response = response.substring(0, configManager.getMaxResponseLength()) + "...";
                    player.sendMessage(Component.text(configManager.getMessage("response-truncated", null), NamedTextColor.YELLOW));
                }
                String prefix = configManager.getMessage("suggested-response-prefix", null);
                String command = "/aichat " + originalAIName + " " + response.replace("\"", "\\\"");
                Component message = Component.text(prefix + response)
                        .color(NamedTextColor.WHITE)
                        .clickEvent(ClickEvent.runCommand(command))
                        .hoverEvent(HoverEvent.showText(Component.text(configManager.getMessage("suggested-response-hover", null), NamedTextColor.YELLOW)));
                player.sendMessage(message);
            }
        }
    }
}











