package net.miamicenter.lobbywars;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TimerManager {
    LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    private final Map<Integer, Runnable> onInterruptMap = new HashMap<>();

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
    public void stopTimer(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
        if (onInterruptMap.get(taskId) != null) {
            onInterruptMap.get(taskId).run();
        }
    }
}
