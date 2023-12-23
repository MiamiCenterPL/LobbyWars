package net.miamicenter.lobbywars;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsCache {
    private static final Map<UUID, PlayerStats> cache = new HashMap<>();
    public static PlayerStats getStats(UUID playerUUID) {
        return cache.get(playerUUID);
    }
    public static void putStats(PlayerStats stats) {
        cache.put(stats.getPlayerUUID(), stats);
    }
    public static void removeStats(UUID playerUUID) {
        cache.remove(playerUUID);
    }
    public static void saveAllToDB() {
        for (PlayerStats record : cache.values()){
            DBHelper.getInstance().updatePlayerStats(record.getPlayerUUID(), record);
        }
    }
}

