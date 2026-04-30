package com.ollamachat.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bukkit/Spigot implementation of TaskScheduler.
 * Uses standard Bukkit scheduler API.
 */
public class BukkitScheduler implements TaskScheduler {

    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    @Override
    public void runTask(Plugin plugin, Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runLater(Plugin plugin, Runnable runnable, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runOnEntity(Plugin plugin, Player player, Runnable runnable) {
        // On Bukkit, entity scheduling is same as global scheduling
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runOnEntityLater(Plugin plugin, Player player, Runnable runnable, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
    }

    @Override
    public Object runTaskTimerOnEntity(Plugin plugin, Player player, Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        UUID taskId = UUID.randomUUID();
        activeTasks.put(taskId, task);
        return taskId;
    }

    @Override
    public void cancelTask(Object taskHandle) {
        if (taskHandle instanceof UUID) {
            BukkitTask task = activeTasks.remove(taskHandle);
            if (task != null) {
                task.cancel();
            }
        }
    }
}