package net.miamicenter.lobbywars;

import java.util.UUID;

public class PlayerStats {
    private final UUID playerUUID;
    private int kills;
    private int deaths;
    public PlayerStats(UUID playerUUID, int kills, int deaths) {
        this.playerUUID = playerUUID;
        this.kills = kills;
        this.deaths = deaths;
        //PlayerStatsCache.putStats(this);
    }
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    public int getKills() {
        return kills;
    }
    public int getDeaths() {
        return deaths;
    }
    public void incrementKills() {
        kills++;
    }
    public void incrementDeaths() {
        deaths++;
    }
}
