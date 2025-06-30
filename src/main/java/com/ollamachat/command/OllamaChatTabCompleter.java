package com.ollamachat.command;

import com.ollamachat.core.Ollamachat;
import com.ollamachat.core.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OllamaChatTabCompleter implements TabCompleter {
    private final Ollamachat plugin;
    private final ConfigManager configManager;

    public OllamaChatTabCompleter(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("ollamachat")) {
            if (args.length == 1) {
                List<String> subCommands = new ArrayList<>();
                if (sender.hasPermission("ollamachat.reload")) {
                    subCommands.add("reload");
                }
                if (sender.hasPermission("ollamachat.toggle")) {
                    subCommands.add("toggle");
                }
                if (sender.hasPermission("ollamachat.prompt.set") ||
                        sender.hasPermission("ollamachat.prompt.delete") ||
                        sender.hasPermission("ollamachat.prompt.list") ||
                        sender.hasPermission("ollamachat.prompt.select")) {
                    subCommands.add("prompt");
                }
                if (sender instanceof Player && (
                        sender.hasPermission("ollamachat.conversation.new") ||
                                sender.hasPermission("ollamachat.conversation.select") ||
                                sender.hasPermission("ollamachat.conversation.delete") ||
                                sender.hasPermission("ollamachat.conversation.list"))) {
                    subCommands.add("conversation");
                }
                return filterCompletions(subCommands, args[0]);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle") && sender.hasPermission("ollamachat.toggle")) {
                List<String> aiNames = new ArrayList<>();
                aiNames.add("ollama");
                aiNames.addAll(configManager.getOtherAIConfigs().keySet());
                return filterCompletions(aiNames, args[1]);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("prompt") && (
                    sender.hasPermission("ollamachat.prompt.set") ||
                            sender.hasPermission("ollamachat.prompt.delete") ||
                            sender.hasPermission("ollamachat.prompt.list") ||
                            sender.hasPermission("ollamachat.prompt.select"))) {
                List<String> promptSubCommands = new ArrayList<>();
                if (sender.hasPermission("ollamachat.prompt.set")) {
                    promptSubCommands.add("set");
                }
                if (sender.hasPermission("ollamachat.prompt.delete")) {
                    promptSubCommands.add("delete");
                }
                if (sender.hasPermission("ollamachat.prompt.list")) {
                    promptSubCommands.add("list");
                }
                if (sender.hasPermission("ollamachat.prompt.select")) {
                    promptSubCommands.add("select");
                    promptSubCommands.add("clear");
                }
                return filterCompletions(promptSubCommands, args[1]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("prompt") && (
                    args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("select")) &&
                    (sender.hasPermission("ollamachat.prompt.delete") || sender.hasPermission("ollamachat.prompt.select"))) {
                return filterCompletions(new ArrayList<>(configManager.getPrompts().keySet()), args[2]);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("conversation") && sender instanceof Player &&
                    (sender.hasPermission("ollamachat.conversation.new") ||
                            sender.hasPermission("ollamachat.conversation.select") ||
                            sender.hasPermission("ollamachat.conversation.delete") ||
                            sender.hasPermission("ollamachat.conversation.list"))) {
                List<String> convSubCommands = new ArrayList<>();
                if (sender.hasPermission("ollamachat.conversation.new")) {
                    convSubCommands.add("new");
                }
                if (sender.hasPermission("ollamachat.conversation.select")) {
                    convSubCommands.add("select");
                }
                if (sender.hasPermission("ollamachat.conversation.delete")) {
                    convSubCommands.add("delete");
                }
                if (sender.hasPermission("ollamachat.conversation.list")) {
                    convSubCommands.add("list");
                }
                return filterCompletions(convSubCommands, args[1]);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("conversation") && sender instanceof Player) {
                List<String> aiNames = new ArrayList<>();
                aiNames.add("ollama");
                aiNames.addAll(configManager.getOtherAIConfigs().keySet());
                return filterCompletions(aiNames, args[2]);
            } else if (args.length == 4 && args[0].equalsIgnoreCase("conversation") &&
                    (args[1].equalsIgnoreCase("select") || args[1].equalsIgnoreCase("delete")) && sender instanceof Player &&
                    (sender.hasPermission("ollamachat.conversation.select") || sender.hasPermission("ollamachat.conversation.delete"))) {
                String aiName = args[2];
                Map<String, String> conversations = plugin.getChatHistoryManager().listConversations(((Player) sender).getUniqueId(), aiName);
                return filterCompletions(new ArrayList<>(conversations.values()), args[3]);
            }
        } else if (command.getName().equalsIgnoreCase("aichat") && sender.hasPermission("ollamachat.use")) {
            if (args.length == 1) {
                List<String> aiNames = new ArrayList<>();
                aiNames.add("ollama");
                aiNames.addAll(configManager.getOtherAIConfigs().keySet());
                return filterCompletions(aiNames, args[0]);
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}

