package com.ollamachat;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProgressManager {
    private final Ollamachat plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public ProgressManager(Ollamachat plugin) {
        this.plugin = plugin;
    }

    public void startProgress(Player player, String title, BarColor color, BarStyle style) {
        UUID uuid = player.getUniqueId();
        cleanup(player);

        if (plugin.getConfig().getString("progress-display.type", "bossbar").equalsIgnoreCase("bossbar")) {
            handleBossBarProgress(player, uuid, title, color, style);
        } else {
            handleActionBarProgress(player, uuid);
        }
    }

    private void handleBossBarProgress(Player player, UUID uuid, String title, BarColor color, BarStyle style) {
        BossBar bossBar = Bukkit.createBossBar(
                plugin.getMessage("generating-status", Map.of("progress", "0")),
                color,
                style
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(0.0); // Set initial progress to 0%
        bossBar.setVisible(true);
        bossBars.put(uuid, bossBar);

        long interval = 20L * plugin.getConfig().getInt("progress-display.update-interval", 1);
        tasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cleanup(player);
            }
        }, 0, interval));
    }

    private void handleActionBarProgress(Player player, UUID uuid) {
        long interval = 20L * plugin.getConfig().getInt("progress-display.update-interval", 1);
        tasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cleanup(player);
                return;
            }
            player.sendActionBar(plugin.getMessage("generating-status", Map.of("progress", "0")));
        }, 0, interval));
    }

    public void complete(Player player) {
        UUID uuid = player.getUniqueId();
        if (bossBars.containsKey(uuid)) {
            BossBar bossBar = bossBars.get(uuid);
            bossBar.setProgress(1.0); // Set progress to 100% on completion
            bossBar.setTitle(plugin.getMessage("complete-status", null));
            bossBar.setColor(BarColor.GREEN);
            Bukkit.getScheduler().runTaskLater(plugin, () -> cleanup(player), 40L);
        } else {
            // For action bar, just cleanup as progress is handled in handleActionBarProgress
            Bukkit.getScheduler().runTaskLater(plugin, () -> cleanup(player), 40L);
        }
    }

    public void error(Player player) {
        UUID uuid = player.getUniqueId();
        if (bossBars.containsKey(uuid)) {
            BossBar bossBar = bossBars.get(uuid);
            bossBar.setTitle(plugin.getMessage("error-status", null));
            bossBar.setColor(BarColor.RED);
            Bukkit.getScheduler().runTaskLater(plugin, () -> cleanup(player), 40L);
        }
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) {
            tasks.get(uuid).cancel();
            tasks.remove(uuid);
        }
        if (bossBars.containsKey(uuid)) {
            bossBars.get(uuid).removeAll();
            bossBars.remove(uuid);
        }
    }
}

