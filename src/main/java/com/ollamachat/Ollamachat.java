package com.ollamachat;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Ollamachat extends JavaPlugin implements Listener {

    private HttpClient httpClient;
    private Gson gson;
    private String apiUrl;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();

        httpClient = HttpClient.newHttpClient();
        gson = new Gson();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ollamareload").setExecutor(this);
    }

    private void reloadConfigValues() {
        FileConfiguration config = getConfig();
        apiUrl = config.getString("ollama-api-url", "http://localhost:11434/api/generate");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        String triggerPrefix = getConfig().getString("trigger-prefix", "@bot ");

        if (message.startsWith(triggerPrefix)) {
            event.setCancelled(true);
            String prompt = message.substring(triggerPrefix.length()).trim();

            if (!prompt.isEmpty()) {
                processQueryAsync(player, prompt);
            }
        }
    }

    private void processQueryAsync(Player player, String prompt) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", getConfig().getString("model"));
                requestBody.put("prompt", prompt);
                requestBody.put("stream", false);

                String jsonRequest = gson.toJson(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    OllamaResponse ollamaResponse = gson.fromJson(
                            response.body(),
                            OllamaResponse.class
                    );
                    sendFormattedResponse(player, ollamaResponse.response);
                } else {
                    sendErrorMessage(player, "Ollama API Error: " + response.body());
                }
            } catch (Exception e) {
                getLogger().severe("Error processing Ollama request: " + e.getMessage());
                sendErrorMessage(player, "Failed to get response from AI");
            }
        });
    }

    private void sendFormattedResponse(Player player, String response) {
        Bukkit.getScheduler().runTask(this, () -> {
            String prefix = getConfig().getString("response-prefix", "[AI] ");
            String formatted = prefix + response.replace("\\n", "\n");

            for (String line : formatted.split("\n")) {
                if (!line.trim().isEmpty()) {
                    player.sendMessage(line);
                }
            }
        });
    }

    private void sendErrorMessage(Player player, String message) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.sendMessage("§c" + message);
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ollamareload")) {
            reloadConfig();
            reloadConfigValues();
            sender.sendMessage("§aOllama configuration reloaded!");
            return true;
        }
        return false;
    }

    private static class OllamaResponse {
        String response;
    }
}
