package com.ollamachat.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Utility class for accessing the appropriate scheduler implementation
 * based on the server platform (Bukkit/Spigot vs Folia).
 */
public class SchedulerUtils {

    private static Boolean isFolia = null;
    private static TaskScheduler scheduler = null;

    /**
     * Detect if the server is running Folia.
     * @return true if Folia API is available
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                // Check for Folia-specific scheduler API
                Bukkit.class.getMethod("getAsyncScheduler");
                isFolia = true;
            } catch (NoSuchMethodException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * Get the appropriate TaskScheduler implementation for the current platform.
     * @return TaskScheduler instance
     */
    public static TaskScheduler getScheduler() {
        if (scheduler == null) {
            if (isFolia()) {
                try {
                    scheduler = (TaskScheduler) Class.forName(
                            "com.ollamachat.scheduler.FoliaScheduler"
                    ).getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    Bukkit.getLogger().severe(
                            "[OllamaChat] Failed to load FoliaScheduler: " + e.getMessage()
                    );
                    scheduler = new BukkitScheduler();
                }
            } else {
                scheduler = new BukkitScheduler();
            }
        }
        return scheduler;
    }

    /**
     * Run a task on the next tick.
     */
    public static void runTask(Plugin plugin, Runnable runnable) {
        getScheduler().runTask(plugin, runnable);
    }

    /**
     * Run a task after a delay.
     */
    public static void runLater(Plugin plugin, Runnable runnable, long ticks) {
        getScheduler().runLater(plugin, runnable, ticks);
    }

    /**
     * Run a task asynchronously.
     */
    public static void runAsync(Plugin plugin, Runnable runnable) {
        getScheduler().runAsync(plugin, runnable);
    }

    /**
     * Run a task on the player's region scheduler.
     */
    public static void runOnEntity(Plugin plugin, Player player, Runnable runnable) {
        getScheduler().runOnEntity(plugin, player, runnable);
    }

    /**
     * Run a task on the player's region scheduler after a delay.
     */
    public static void runOnEntityLater(Plugin plugin, Player player, Runnable runnable, long ticks) {
        getScheduler().runOnEntityLater(plugin, player, runnable, ticks);
    }

    /**
     * Run a repeating task on the player's region scheduler.
     */
    public static Object runTaskTimerOnEntity(Plugin plugin, Player player, Runnable runnable, long delayTicks, long periodTicks) {
        return getScheduler().runTaskTimerOnEntity(plugin, player, runnable, delayTicks, periodTicks);
    }

    /**
     * Cancel a scheduled task.
     */
    public static void cancelTask(Object taskHandle) {
        getScheduler().cancelTask(taskHandle);
    }
}