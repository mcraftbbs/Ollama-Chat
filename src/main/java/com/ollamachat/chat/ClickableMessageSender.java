package com.ollamachat.chat;

import org.bukkit.entity.Player;

public interface ClickableMessageSender {

    void sendClickableMessage(Player player, String text, String command, String hoverText);
}
