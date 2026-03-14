package com.ollamachat.command;

import com.ollamachat.core.Ollamachat;
import com.ollamachat.core.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
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
                if (sender instanceof Player && sender.hasPermission("ollamachat.suggests.toggle")) {
                    subCommands.add("suggests");
                }
                if (sender.hasPermission("ollamachat.suggests-presets.toggle")) {
                    subCommands.add("suggests-presets");
                }
                if (sender.hasPermission("ollamachat.search.toggle") ||
                        sender.hasPermission("ollamachat.search.status") ||
                        sender.hasPermission("ollamachat.search.query") ||
                        sender.hasPermission("ollamachat.search.engine") ||
                        sender.hasPermission("ollamachat.search.setkey") ||
                        sender.hasPermission("ollamachat.search.setcount") ||
                        sender.hasPermission("ollamachat.search.keywords")) {
                    subCommands.add("search");
                }
                return filterCompletions(subCommands, args[0]);
            }
            else if (args.length == 2 && args[0].equalsIgnoreCase("toggle") && sender.hasPermission("ollamachat.toggle")) {
                List<String> aiNames = new ArrayList<>();
                aiNames.add("ollama");
                aiNames.addAll(configManager.getOtherAIConfigs().keySet());
                return filterCompletions(aiNames, args[1]);
            }
            else if (args.length == 2 && args[0].equalsIgnoreCase("prompt") && (
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
            }
            else if (args.length == 3 && args[0].equalsIgnoreCase("prompt") && (
                    args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("select")) &&
                    (sender.hasPermission("ollamachat.prompt.delete") || sender.hasPermission("ollamachat.prompt.select"))) {
                return filterCompletions(new ArrayList<>(configManager.getPrompts().keySet()), args[2]);
            }
            else if (args.length == 2 && args[0].equalsIgnoreCase("conversation") && sender instanceof Player &&
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
            }
            else if (args.length == 3 && args[0].equalsIgnoreCase("conversation") && sender instanceof Player) {
                List<String> aiNames = new ArrayList<>();
                aiNames.add("ollama");
                aiNames.addAll(configManager.getOtherAIConfigs().keySet());
                return filterCompletions(aiNames, args[2]);
            }
            else if (args.length == 4 && args[0].equalsIgnoreCase("conversation") &&
                    (args[1].equalsIgnoreCase("select") || args[1].equalsIgnoreCase("delete")) && sender instanceof Player &&
                    (sender.hasPermission("ollamachat.conversation.select") || sender.hasPermission("ollamachat.conversation.delete"))) {
                String aiName = args[2];
                Map<String, String> conversations = plugin.getChatHistoryManager().listConversations(((Player) sender).getUniqueId(), aiName);
                return filterCompletions(new ArrayList<>(conversations.values()), args[3]);
            }
            else if (args.length == 2 && (args[0].equalsIgnoreCase("suggests") || args[0].equalsIgnoreCase("suggests-presets")) && sender instanceof Player &&
                    (sender.hasPermission("ollamachat.suggests.toggle") || sender.hasPermission("ollamachat.suggests-presets.toggle"))) {
                return filterCompletions(Arrays.asList("on", "off"), args[1]);
            }
            else if (args.length == 2 && args[0].equalsIgnoreCase("search") &&
                    (sender.hasPermission("ollamachat.search.toggle") ||
                            sender.hasPermission("ollamachat.search.status") ||
                            sender.hasPermission("ollamachat.search.query") ||
                            sender.hasPermission("ollamachat.search.engine") ||
                            sender.hasPermission("ollamachat.search.setkey") ||
                            sender.hasPermission("ollamachat.search.setcount") ||
                            sender.hasPermission("ollamachat.search.keywords"))) {

                List<String> searchSubCommands = new ArrayList<>();

                if (sender.hasPermission("ollamachat.search.toggle")) {
                    searchSubCommands.add("toggle");
                }
                if (sender.hasPermission("ollamachat.search.status")) {
                    searchSubCommands.add("status");
                }
                if (sender.hasPermission("ollamachat.search.query")) {
                    searchSubCommands.add("query");
                }
                if (sender.hasPermission("ollamachat.search.engine")) {
                    searchSubCommands.add("engine");
                }
                if (sender.hasPermission("ollamachat.search.setkey")) {
                    searchSubCommands.add("setkey");
                }
                if (sender.hasPermission("ollamachat.search.setcount")) {
                    searchSubCommands.add("setcount");
                }
                if (sender.hasPermission("ollamachat.search.keywords")) {
                    searchSubCommands.add("addkeyword");
                    searchSubCommands.add("removekeyword");
                    searchSubCommands.add("listkeywords");
                }

                return filterCompletions(searchSubCommands, args[1]);
            }
            else if (args.length == 3 && args[0].equalsIgnoreCase("search")) {
                String subCommand = args[1].toLowerCase();

                if (subCommand.equals("engine") && sender.hasPermission("ollamachat.search.engine")) {
                    return filterCompletions(Arrays.asList("bocha", "brave"), args[2]);
                }
                else if (subCommand.equals("setkey") && sender.hasPermission("ollamachat.search.setkey")) {
                    if (args[2].isEmpty()) {
                        return Arrays.asList("bocha", "brave");
                    } else {
                        return filterCompletions(Arrays.asList("bocha", "brave"), args[2]);
                    }
                }
                else if (subCommand.equals("setcount") && sender.hasPermission("ollamachat.search.setcount")) {
                    return filterCompletions(Arrays.asList("5", "10", "15", "20", "25", "30"), args[2]);
                }
                else if (subCommand.equals("addkeyword") && sender.hasPermission("ollamachat.search.keywords")) {
                    return Arrays.asList("<keyword>");
                }
                else if (subCommand.equals("removekeyword") && sender.hasPermission("ollamachat.search.keywords")) {
                    return filterCompletions(configManager.getWebSearchTriggerKeywords(), args[2]);
                }
            }
            else if (args.length == 4 && args[0].equalsIgnoreCase("search") && args[1].equalsIgnoreCase("setkey") &&
                    sender.hasPermission("ollamachat.search.setkey")) {
                String engine = args[2].toLowerCase();
                if (engine.equals("bocha")) {
                    return Arrays.asList("<bocha-api-key>");
                } else if (engine.equals("brave")) {
                    return Arrays.asList("<brave-api-key>");
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("aichat") && sender.hasPermission("ollamachat.use")) {
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