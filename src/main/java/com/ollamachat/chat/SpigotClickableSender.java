package com.ollamachat.chat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class SpigotClickableSender implements ClickableMessageSender {

    @Override
    public void sendClickableMessage(Player player, String text, String command, String hoverText) {
        if (!player.isOnline()) {
            return;
        }

        TextComponent message = new TextComponent(TextComponent.fromLegacyText(text));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText)
                        .color(ChatColor.YELLOW)
                        .create()));

        player.spigot().sendMessage(message);
    }
}
