package com.ollamachat.format;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormatter {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)\\}");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");

    private final MarkdownFormatter markdownFormatter;
    private final boolean useMarkdown;

    public MessageFormatter() {
        this(true);
    }

    public MessageFormatter(boolean useMarkdown) {
        this.useMarkdown = useMarkdown;
        this.markdownFormatter = new MarkdownFormatter();
    }

    public String format(String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }

        String result = message;

        // Replace placeholders first
        if (placeholders != null) {
            result = replacePlaceholders(result, placeholders);
        }

        // Convert color codes (support both & and § formats)
        result = ChatColor.translateAlternateColorCodes('&', result);

        // Convert Markdown if enabled
        if (useMarkdown) {
            result = markdownFormatter.format(result);
        }

        return result;
    }

    public String format(String message) {
        return format(message, null);
    }

    public void sendMessage(Player player, String message, Map<String, String> placeholders) {
        if (player.isOnline()) {
            String formatted = format(message, placeholders);
            // Split by newlines and send each line separately
            String[] lines = formatted.split("\n");
            for (String line : lines) {
                player.sendMessage(line);
            }
        }
    }

    public void sendMessage(Player player, String message) {
        sendMessage(player, message, null);
    }

    public void sendMessages(Player player, String... messages) {
        if (player.isOnline()) {
            for (String message : messages) {
                String formatted = format(message);
                String[] lines = formatted.split("\n");
                for (String line : lines) {
                    player.sendMessage(line);
                }
            }
        }
    }

    public String error(String message) {
        return ChatColor.RED + ChatColor.BOLD.toString() + "[!] " +
                ChatColor.RED + format(message);
    }

    public String success(String message) {
        return ChatColor.GREEN + ChatColor.BOLD.toString() + "[OK] " +
                ChatColor.GREEN + format(message);
    }

    public String info(String message) {
        return ChatColor.BLUE + ChatColor.BOLD.toString() + "[i] " +
                ChatColor.BLUE + format(message);
    }

    public String warning(String message) {
        return ChatColor.YELLOW + ChatColor.BOLD.toString() + "[!] " +
                ChatColor.YELLOW + format(message);
    }

    public String debug(String message) {
        return ChatColor.GRAY + ChatColor.ITALIC.toString() + format(message);
    }

    public String formatAIResponse(String response) {
        return ChatColor.AQUA + ChatColor.BOLD.toString() + "AI " +
                ChatColor.DARK_GRAY + "" + ChatColor.WHITE +
                markdownFormatter.format(response);
    }

    public String formatPlayerMessage(String playerName, String message) {
        return ChatColor.GOLD + playerName + ChatColor.DARK_GRAY + " " +
                ChatColor.WHITE + message;
    }

    public String formatChatHeader(String title) {
        return ChatColor.GRAY + ChatColor.STRIKETHROUGH.toString() + "     " +
                ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " " + title + " " +
                ChatColor.GRAY + ChatColor.STRIKETHROUGH.toString() + "     " +
                ChatColor.RESET;
    }

    public String formatChatFooter() {
        return ChatColor.GRAY + ChatColor.STRIKETHROUGH.toString() +
                "                              " + ChatColor.RESET;
    }

    public String formatSearchResult(int index, String title, String url, String snippet) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append(ChatColor.BOLD).append("#").append(index).append(" ");
        sb.append(ChatColor.YELLOW).append(title).append("\n");
        sb.append(ChatColor.GRAY).append("  ").append(url).append("\n");
        sb.append(ChatColor.WHITE).append("  ");

        // Truncate snippet if too long
        if (snippet.length() > 100) {
            snippet = snippet.substring(0, 97) + "...";
        }
        sb.append(snippet);
        return sb.toString();
    }

    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.getOrDefault(key, "{" + key + "}");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static Map<String, String> placeholders(String... keysAndValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length - 1; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}