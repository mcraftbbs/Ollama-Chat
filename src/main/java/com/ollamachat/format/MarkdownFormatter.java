package com.ollamachat.format;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownFormatter {

    // Regex patterns for Markdown elements
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern BOLD_ALT_PATTERN = Pattern.compile("__(.+?)__");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern ITALIC_ALT_PATTERN = Pattern.compile("_(.+?)_");
    private static final Pattern BOLD_ITALIC_PATTERN = Pattern.compile("\\*\\*\\*(.+?)\\*\\*\\*");
    private static final Pattern BOLD_ITALIC_ALT_PATTERN = Pattern.compile("___(.+?)___");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(.+?)~~");
    private static final Pattern UNDERLINE_PATTERN = Pattern.compile("__(.+?)__");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(.*?)```", Pattern.DOTALL);
    private static final Pattern HEADER_PATTERN = Pattern.compile("(?m)^(#{1,6})\\s+(.+?)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("(?m)^[\\-\\*\\+]\\s+(.+?)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("(?m)^\\d+\\.\\s+(.+?)$");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("(?m)^>\\s+(.+?)$");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("(?m)^(\\-{3,}|\\*{3,}|_{3,})$");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");

    private final boolean enableColors;
    private final boolean enableFormatting;
    private final int maxLineLength;

    public MarkdownFormatter() {
        this(true, true, 256);
    }

    public MarkdownFormatter(boolean enableColors, boolean enableFormatting, int maxLineLength) {
        this.enableColors = enableColors;
        this.enableFormatting = enableFormatting;
        this.maxLineLength = maxLineLength;
    }

    public String format(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String result = markdown;

        // Process code blocks first to prevent formatting inside them
        result = processCodeBlocks(result);

        // Process inline code
        result = processInlineCode(result);

        // Process headers
        result = processHeaders(result);

        // Process horizontal rules
        result = processHorizontalRules(result);

        // Process blockquotes
        result = processBlockquotes(result);

        // Process lists
        result = processUnorderedLists(result);
        result = processOrderedLists(result);

        // Process text formatting
        if (enableFormatting) {
            result = processBoldItalic(result);
            result = processBold(result);
            result = processItalic(result);
            result = processStrikethrough(result);
            result = processUnderline(result);
        }

        // Process links
        result = processLinks(result);

        // Clean up any remaining Markdown symbols
        result = cleanRemainingMarkdown(result);

        // Wrap long lines
        result = wrapLongLines(result);

        return result;
    }

    private String processCodeBlocks(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String code = matcher.group(1).trim();
            // Replace with indented, colored code
            String formatted = ChatColor.GRAY + "  " + code.replace("\n", "\n  ") + ChatColor.RESET;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processInlineCode(String text) {
        Matcher matcher = CODE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String code = matcher.group(1);
            String formatted = ChatColor.GRAY + code + ChatColor.RESET;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processHeaders(String text) {
        Matcher matcher = HEADER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int level = matcher.group(1).length();
            String content = matcher.group(2);

            ChatColor color;
            switch (level) {
                case 1:
                    color = ChatColor.GOLD;
                    break;
                case 2:
                    color = ChatColor.YELLOW;
                    break;
                default:
                    color = ChatColor.WHITE;
                    break;
            }

            String formatted = color + ChatColor.BOLD.toString() + content + ChatColor.RESET;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processHorizontalRules(String text) {
        Matcher matcher = HORIZONTAL_RULE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String rule = ChatColor.GRAY + ChatColor.STRIKETHROUGH.toString() +
                    "                    " + ChatColor.RESET;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(rule));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processBlockquotes(String text) {
        Matcher matcher = BLOCKQUOTE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            String formatted = ChatColor.GRAY + ChatColor.ITALIC.toString() +
                    "| " + content + ChatColor.RESET;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processUnorderedLists(String text) {
        Matcher matcher = UNORDERED_LIST_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            String formatted = ChatColor.WHITE + "  " + ChatColor.BOLD + "•" +
                    ChatColor.RESET + " " + content;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processOrderedLists(String text) {
        Matcher matcher = ORDERED_LIST_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        int counter = 1;

        // Find all matches first to apply correct numbering
        while (matcher.find()) {
            String content = matcher.group(1);
            String formatted = ChatColor.WHITE + "  " + ChatColor.BOLD + counter + "." +
                    ChatColor.RESET + " " + content;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
            counter++;
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processBoldItalic(String text) {
        // Process *** text ***
        text = applyFormatting(text, BOLD_ITALIC_PATTERN,
                ChatColor.BOLD.toString() + ChatColor.ITALIC.toString());
        // Process ___ text ___
        text = applyFormatting(text, BOLD_ITALIC_ALT_PATTERN,
                ChatColor.BOLD.toString() + ChatColor.ITALIC.toString());
        return text;
    }

    private String processBold(String text) {
        // Process ** text **
        text = applyFormatting(text, BOLD_PATTERN, ChatColor.BOLD.toString());
        // Process __ text __ (but not ___ which is bold italic)
        text = applyFormatting(text, UNDERLINE_PATTERN, ChatColor.BOLD.toString());
        return text;
    }

    private String processItalic(String text) {
        // Process * text * (but not ** which is bold)
        text = applyFormatting(text, ITALIC_PATTERN, ChatColor.ITALIC.toString());
        // Process _ text _ (but not __ which is bold or ___ which is bold italic)
        text = applyFormatting(text, ITALIC_ALT_PATTERN, ChatColor.ITALIC.toString());
        return text;
    }

    private String processStrikethrough(String text) {
        return applyFormatting(text, STRIKETHROUGH_PATTERN, ChatColor.STRIKETHROUGH.toString());
    }

    private String processUnderline(String text) {
        // Note: We're repurposing __ for underline since ** is used for bold
        // Actually, __ conflicts with bold, so we'll use a different approach
        return text; // Underline is handled in processBold as an alternative
    }

    private String processLinks(String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String linkText = matcher.group(1);
            String url = matcher.group(2);

            // Format as: linkText (url) since Minecraft doesn't support clickable links in chat
            String formatted = ChatColor.AQUA + linkText + ChatColor.GRAY +
                    " (" + url + ")" + ChatColor.RESET;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String applyFormatting(String text, Pattern pattern, String formattingCode) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            String formatted = formattingCode + content + ChatColor.RESET;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String cleanRemainingMarkdown(String text) {
        // Remove leftover Markdown formatting symbols
        text = text.replaceAll("(?<!\\\\)[\\*\\_\\~]{1,3}", "");
        // Replace escaped symbols
        text = text.replaceAll("\\\\([\\*\\_\\~\\[\\]\\(\\)\\`\\#])", "$1");
        return text;
    }

    private String wrapLongLines(String text) {
        if (maxLineLength <= 0) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");

        for (String line : lines) {
            // Skip formatting codes when calculating length
            String stripped = ChatColor.stripColor(line);

            if (stripped.length() <= maxLineLength) {
                result.append(line).append("\n");
            } else {
                // Split long lines
                int start = 0;
                while (start < line.length()) {
                    int end = Math.min(start + maxLineLength, line.length());
                    // Try to break at a space
                    if (end < line.length()) {
                        int lastSpace = line.lastIndexOf(' ', end);
                        if (lastSpace > start) {
                            end = lastSpace;
                        }
                    }
                    result.append(line, start, end).append("\n");
                    start = end;
                    // Skip leading spaces on continuation lines
                    while (start < line.length() && line.charAt(start) == ' ') {
                        start++;
                    }
                }
            }
        }

        return result.toString().trim();
    }

    public static String stripMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String text = markdown;

        // Remove code blocks
        text = text.replaceAll("```.*?```", "");
        // Remove inline code
        text = text.replaceAll("`(.+?)`", "$1");
        // Remove headers
        text = text.replaceAll("(?m)^#{1,6}\\s+", "");
        // Remove bold/italic
        text = text.replaceAll("\\*{1,3}(.+?)\\*{1,3}", "$1");
        text = text.replaceAll("_{1,3}(.+?)_{1,3}", "$1");
        // Remove strikethrough
        text = text.replaceAll("~~(.+?)~~", "$1");
        // Remove links, keep text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1");
        // Remove list markers
        text = text.replaceAll("(?m)^[\\-\\*\\+]\\s+", "");
        text = text.replaceAll("(?m)^\\d+\\.\\s+", "");
        // Remove blockquotes
        text = text.replaceAll("(?m)^>\\s+", "");
        // Remove horizontal rules
        text = text.replaceAll("(?m)^[\\-\\*_]{3,}$", "");

        return text.trim();
    }

    public static String formatBulletList(String... items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append(ChatColor.WHITE).append("  ").append(ChatColor.BOLD)
                    .append("•").append(ChatColor.RESET).append(" ").append(item).append("\n");
        }
        return sb.toString().trim();
    }

    public static String formatNumberedList(String... items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            sb.append(ChatColor.WHITE).append("  ").append(ChatColor.BOLD)
                    .append(i + 1).append(".").append(ChatColor.RESET)
                    .append(" ").append(items[i]).append("\n");
        }
        return sb.toString().trim();
    }

    public static String formatHeader(String text, int level) {
        ChatColor color;
        switch (level) {
            case 1:
                color = ChatColor.GOLD;
                break;
            case 2:
                color = ChatColor.YELLOW;
                break;
            default:
                color = ChatColor.WHITE;
                break;
        }
        return color + ChatColor.BOLD.toString() + text + ChatColor.RESET;
    }

    public static String formatSeparator() {
        return ChatColor.GRAY + ChatColor.STRIKETHROUGH.toString() +
                "                              " + ChatColor.RESET;
    }
}