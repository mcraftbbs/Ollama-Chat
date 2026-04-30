package com.ollamachat.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Folia implementation of TaskScheduler.
 * Uses reflection to handle API differences between Folia versions.
 */
public class FoliaScheduler implements TaskScheduler {

    private final Map<UUID, Object> activeTasks = new ConcurrentHashMap<>();

    @Override
    public void runTask(Plugin plugin, Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(plugin, (task) -> runnable.run());
    }

    @Override
    public void runLater(Plugin plugin, Runnable runnable, long ticks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (task) -> runnable.run(), ticks);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> runnable.run());
    }

    @Override
    public void runOnEntity(Plugin plugin, Player player, Runnable runnable) {
        player.getScheduler().run(plugin, (task) -> runnable.run(), null);
    }

    @Override
    public void runOnEntityLater(Plugin plugin, Player player, Runnable runnable, long ticks) {
        player.getScheduler().runDelayed(plugin, (task) -> runnable.run(), null, ticks);
    }

    @Override
    public Object runTaskTimerOnEntity(Plugin plugin, Player player, Runnable runnable, long delayTicks, long periodTicks) {
        // Use a simpler approach: schedule the next execution manually
        // This avoids complex API compatibility issues
        UUID taskId = UUID.randomUUID();
        boolean[] active = {true};

        // Store active state instead of direct task reference
        activeTasks.put(taskId, active);

        // Create recursive scheduling
        Runnable scheduledTask = new Runnable() {
            @Override
            public void run() {
                if (!active[0] || !player.isOnline()) {
                    activeTasks.remove(taskId);
                    return;
                }

                try {
                    runnable.run();
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[OllamaChat] Error in scheduled task: " + e.getMessage());
                }

                // Schedule next execution
                if (active[0] && player.isOnline()) {
                    player.getScheduler().runDelayed(
                            plugin,
                            (task) -> this.run(),
                            null,
                            periodTicks
                    );
                }
            }
        };

        // Initial delayed execution
        player.getScheduler().runDelayed(
                plugin,
                (task) -> scheduledTask.run(),
                null,
                delayTicks
        );

        return taskId;
    }

    @Override
    public void cancelTask(Object taskHandle) {
        if (taskHandle instanceof UUID) {
            Object task = activeTasks.remove(taskHandle);
            if (task instanceof boolean[]) {
                ((boolean[]) task)[0] = false;
            }
        }
    }
}