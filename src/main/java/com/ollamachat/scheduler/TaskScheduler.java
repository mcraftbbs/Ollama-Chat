package com.ollamachat.scheduler;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Interface for abstracting scheduler operations across different server implementations.
 * Provides unified API for Bukkit, Paper, and Folia schedulers.
 */
public interface TaskScheduler {

    /**
     * Run a task on the next server tick.
     * @param plugin The plugin instance
     * @param runnable The task to run
     */
    void runTask(Plugin plugin, Runnable runnable);

    /**
     * Run a task after a specified delay.
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @param ticks Delay in ticks
     */
    void runLater(Plugin plugin, Runnable runnable, long ticks);

    /**
     * Run a task asynchronously.
     * @param plugin The plugin instance
     * @param runnable The task to run
     */
    void runAsync(Plugin plugin, Runnable runnable);

    /**
     * Run a task on the entity's scheduler (region-specific on Folia).
     * @param plugin The plugin instance
     * @param player The target player
     * @param runnable The task to run
     */
    void runOnEntity(Plugin plugin, Player player, Runnable runnable);

    /**
     * Run a task on the entity's scheduler after a delay.
     * @param plugin The plugin instance
     * @param player The target player
     * @param runnable The task to run
     * @param ticks Delay in ticks
     */
    void runOnEntityLater(Plugin plugin, Player player, Runnable runnable, long ticks);

    /**
     * Run a repeating task on the entity's region scheduler.
     * @param plugin The plugin instance
     * @param player The target player
     * @param runnable The task to run
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     * @return A cancellable task handle
     */
    Object runTaskTimerOnEntity(Plugin plugin, Player player, Runnable runnable, long delayTicks, long periodTicks);

    /**
     * Cancel a scheduled task.
     * @param taskHandle The task handle returned from scheduling methods
     */
    void cancelTask(Object taskHandle);
}