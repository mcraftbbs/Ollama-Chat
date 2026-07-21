package com.ollamachat.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class BukkitClickableSender implements ClickableMessageSender {

    private static Boolean useSnbtFormat = null;
    private static boolean loggedWarning = false;

    @Override
    public void sendClickableMessage(Player player, String text, String command, String hoverText) {
        if (!player.isOnline()) {
            return;
        }

        String cleanText = stripColorCodes(text);

        // Try primary format first
        String component = buildComponent(cleanText, command, hoverText);
        if (tryDispatch(player, component)) {
            return;
        }

        // Fallback: try the other format
        String otherComponent = buildOtherFormatComponent(cleanText, command, hoverText);
        if (tryDispatch(player, otherComponent)) {
            if (!loggedWarning) {
                loggedWarning = true;
                Bukkit.getLogger().log(Level.WARNING,
                        "[OllamaChat] Using fallback format for tellraw. Consider updating the server.");
            }
            return;
        }

        // Ultimate fallback: send plain text
        player.sendMessage(text);
    }

    private boolean tryDispatch(Player player, String component) {
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tellraw " + player.getName() + " " + component);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildComponent(String text, String command, String hoverText) {
        if (isSnbtFormat()) {
            return buildSnbt(text, command, hoverText);
        }
        return buildJson(text, command, hoverText);
    }

    private String buildOtherFormatComponent(String text, String command, String hoverText) {
        if (isSnbtFormat()) {
            return buildJson(text, command, hoverText);
        }
        return buildSnbt(text, command, hoverText);
    }

    private static boolean isSnbtFormat() {
        if (useSnbtFormat != null) {
            return useSnbtFormat;
        }

        try {
            String version = Bukkit.getBukkitVersion();
            if (version == null || version.isEmpty()) {
                useSnbtFormat = false;
                return false;
            }

            String cleanVersion = version.split("-")[0];
            String[] parts = cleanVersion.split("\\.");
            if (parts.length < 2) {
                useSnbtFormat = false;
                return false;
            }

            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);

            // Versions 26.x+, 27.x+ are always >= 1.21.5
            if (major > 1) {
                useSnbtFormat = true;
                return true;
            }

            // For 1.x versions, check if >= 1.21.5
            if (minor > 21) {
                useSnbtFormat = true;
            } else if (minor < 21) {
                useSnbtFormat = false;
            } else {
                // minor == 21, check patch
                if (parts.length < 3) {
                    useSnbtFormat = false;
                } else {
                    int patch = Integer.parseInt(parts[2]);
                    useSnbtFormat = patch >= 5;
                }
            }
        } catch (Exception e) {
            useSnbtFormat = false;
        }
        return useSnbtFormat;
    }

    private String buildJson(String text, String command, String hoverText) {
        return "{\"text\":\"" + escape(text) + "\""
                + ",\"clickEvent\":{\"action\":\"run_command\",\"value\":\""
                + escape(command) + "\"}"
                + ",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\""
                + escape(hoverText) + "\"}}";
    }

    private String buildSnbt(String text, String command, String hoverText) {
        return "{text:\"" + escape(text) + "\""
                + ",click_event:{action:\"run_command\",command:\""
                + escape(command) + "\"}"
                + ",hover_event:{action:\"show_text\",value:\""
                + escape(hoverText) + "\"}}";
    }

    private String stripColorCodes(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
