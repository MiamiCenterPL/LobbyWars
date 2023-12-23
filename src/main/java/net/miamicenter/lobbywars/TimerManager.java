package net.miamicenter.lobbywars;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TimerManager {
    private static TimerManager instance;
    private final LobbyWars plugin;
    private final Map<Integer, Runnable> onInterruptMap;
    private TimerManager() {
        this.plugin = LobbyWars.getPlugin(LobbyWars.class);
        this.onInterruptMap = new HashMap<>();
    }
    public static TimerManager getInstance() {
        if (instance == null){
            instance = new TimerManager();
        }
        return instance;
    }
    /**
     * A function to start an advanced timer with callbacks;
     *      @param timeStep Execute function every %interval% ticks,
     *                      This function gets int duration as first parameter (time remaining for timer to finish)
     *      @param interval Execute timeStep function on interval (ticks)
     *      @param callback Execute callback function when the timer hits 0 (finishes)
     *      @param onInterrupt Execute function when timer is interrupted (stopped)
     *
     * @return taskID of newly created task, keep it in case you want to manually stop the timer at some point.
     */
    public int startTimer(int initialDuration, double interval, Consumer<Integer> timeStep, Runnable callback, Runnable onInterrupt) {
        final int[] duration = {initialDuration};

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (duration[0] > 0) {
                if (timeStep!= null && duration[0] % interval == 0) timeStep.accept(duration[0]);
                duration[0]--;
            } else {
                callback.run();
            }
        }, 0L, 1L).getTaskId();
        onInterruptMap.put(taskId, onInterrupt);
        return taskId;
    }
    /**
     * Stop timer and execute it's onInterrupt function (if exists)
     * @param taskId a TaskID of task you want to end
     */
    public void stopTimer(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
        if (onInterruptMap.get(taskId) != null) {
            onInterruptMap.get(taskId).run();
        }
    }
}
