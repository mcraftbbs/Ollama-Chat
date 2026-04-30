package com.ollamachat;

import com.ollamachat.core.Ollamachat;
import com.ollamachat.core.ConfigManager;
import com.ollamachat.scheduler.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProgressManager {
    private final Ollamachat plugin;
    private final ConfigManager configManager;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Object> tasks = new HashMap<>();

    public ProgressManager(Ollamachat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
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
                configManager.getMessage("generating-status", Map.of("progress", "0")),
                color,
                style
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(0.0);
        bossBar.setVisible(true);
        bossBars.put(uuid, bossBar);

        long interval = 20L * plugin.getConfig().getInt("progress-display.update-interval", 1);
        // Ensure minimum interval of 1 tick
        interval = Math.max(1, interval);

        // Use SchedulerUtils for Folia compatibility with safe delay of 1 tick
        Object taskHandle = SchedulerUtils.runTaskTimerOnEntity(
                plugin,
                player,
                () -> {
                    if (!player.isOnline()) {
                        cleanup(player);
                    }
                },
                1, // Use 1 tick delay instead of 0 to avoid Folia error
                interval
        );

        tasks.put(uuid, taskHandle);
    }

    private void handleActionBarProgress(Player player, UUID uuid) {
        long interval = 20L * plugin.getConfig().getInt("progress-display.update-interval", 1);
        // Ensure minimum interval of 1 tick
        interval = Math.max(1, interval);

        // Use SchedulerUtils for Folia compatibility with safe delay of 1 tick
        Object taskHandle = SchedulerUtils.runTaskTimerOnEntity(
                plugin,
                player,
                () -> {
                    if (!player.isOnline()) {
                        cleanup(player);
                        return;
                    }
                    player.sendActionBar(
                            configManager.getMessage("generating-status", Map.of("progress", "0"))
                    );
                },
                1, // Use 1 tick delay instead of 0 to avoid Folia error
                interval
        );

        tasks.put(uuid, taskHandle);
    }

    public void complete(Player player) {
        UUID uuid = player.getUniqueId();
        if (bossBars.containsKey(uuid)) {
            BossBar bossBar = bossBars.get(uuid);
            bossBar.setProgress(1.0);
            bossBar.setTitle(configManager.getMessage("complete-status", null));
            bossBar.setColor(BarColor.GREEN);
        }

        // Schedule cleanup after 2 seconds (40 ticks)
        SchedulerUtils.runOnEntityLater(
                plugin,
                player,
                () -> cleanup(player),
                40L
        );
    }

    public void error(Player player) {
        UUID uuid = player.getUniqueId();
        if (bossBars.containsKey(uuid)) {
            BossBar bossBar = bossBars.get(uuid);
            bossBar.setTitle(configManager.getMessage("error-status", null));
            bossBar.setColor(BarColor.RED);
        }

        // Schedule cleanup after 2 seconds (40 ticks)
        SchedulerUtils.runOnEntityLater(
                plugin,
                player,
                () -> cleanup(player),
                40L
        );
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) {
            SchedulerUtils.cancelTask(tasks.get(uuid));
            tasks.remove(uuid);
        }
        if (bossBars.containsKey(uuid)) {
            bossBars.get(uuid).removeAll();
            bossBars.remove(uuid);
        }
    }
}