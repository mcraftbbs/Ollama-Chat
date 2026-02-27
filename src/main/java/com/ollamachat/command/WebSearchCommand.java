package com.ollamachat.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.ollamachat.chat.WebSearchHandler;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.core.Ollamachat;

/**
 * Web Search Command Handler
 * Allows players to search the web using Brave Search API
 */
public class WebSearchCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final WebSearchHandler webSearchHandler;

    public WebSearchCommand(Ollamachat plugin) {
        this.configManager = plugin.getConfigManager();
        this.webSearchHandler = plugin.getWebSearchHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only", null));
            return true;
        }

        Player player = (Player) sender;

        if (!webSearchHandler.isWebSearchEnabled()) {
            player.sendMessage(configManager.getMessage("error-prefix", null) + "Web search is not enabled!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(configManager.getMessage("search-empty-query", null));
            return true;
        }

        // Handle different search commands
        if (label.equalsIgnoreCase("websearch")) {
            String query = String.join(" ", args);
            webSearchHandler.processWebSearchMessage(player, "/search " + query);
        }

        return true;
    }
}
