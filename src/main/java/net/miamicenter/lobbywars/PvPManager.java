package net.miamicenter.lobbywars;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PvPManager {
    private static PvPManager instance;
    private final LobbyWars plugin;
    private final ItemManager itemManager;
    private final TimerManager timerManager;
    private final DBHelper dbHelper;
    private final NamespacedKey triggerPvpTaskKey;
    private final NamespacedKey canPvpKey;
    private PvPManager() {
        this.plugin = LobbyWars.getPlugin(LobbyWars.class);
        this.itemManager = ItemManager.getInstance();
        this.timerManager = TimerManager.getInstance();
        this.dbHelper = DBHelper.getInstance();
        this.triggerPvpTaskKey = CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin);
        this.canPvpKey = CustomAttributes.CAN_PVP.getNamespacedKey(plugin);
    }
    public static PvPManager getInstance() {
        if (instance == null) {
            instance = new PvPManager();
        }
        return instance;
    }
    //Booleans;
    /**
     * @return true if player has pending PVP Task, but is not yet in PVP mode
     */
    public boolean isTriggerPvpTaskRunning(Player player) {
        return player.getPersistentDataContainer().has(triggerPvpTaskKey, PersistentDataType.INTEGER);
    }
    /**
     * @return taskID of pending PVP Task
     */
    public int triggerPvPTaskID(Player player) {
        return player.getPersistentDataContainer().get(triggerPvpTaskKey, PersistentDataType.INTEGER);
    }
    /**
     * Remove player's PVP task and stop it's timer
     */
    public void removePvPTask(Player player) {
        timerManager.stopTimer(triggerPvPTaskID(player));
        player.getPersistentDataContainer().remove(triggerPvpTaskKey);
    }
    /**
     * Set's player PVP task to a container
     * @param player - Player instance
     * @param taskID - A taskID to be saved
     */
    public void setPvPTaskID(Player player, int taskID){
        player.getPersistentDataContainer().set(triggerPvpTaskKey, PersistentDataType.INTEGER, taskID);
    }
    /**
     * Make it so player can damage other players with this attribute.
     */
    public void setPlayerCanDamage(Player player, boolean var){
        player.getPersistentDataContainer().set(canPvpKey, PersistentDataType.BOOLEAN, var);
    }
    /**
     * Can player damage other players (is in PVP mode?)
     */
    public boolean canDamage(Player player){
        if (player.getPersistentDataContainer().has(canPvpKey, PersistentDataType.BOOLEAN)){
            return player.getPersistentDataContainer().get(canPvpKey, PersistentDataType.BOOLEAN);
        } else {
            return false;
        }
    }
    /**
     * Removes players ability to damage other players, clears his armour and changes his sword back to the PVP trigger item
     * This will also send him a message above the hotbar
     */
    public void disablePlayerPvp(Player player) {
        player.getPersistentDataContainer().set(canPvpKey, PersistentDataType.BOOLEAN, false);
        String quitMessage = plugin.getConfigString("quitPVPMessage");
        ActionBarMessage.sendActionBarMessage(player, quitMessage);
        itemManager.clearArmour(player);
        itemManager.givePvPItem(player);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss-SS");
        Date date = new Date();
        dbHelper.saveLogToRedis(
                "[%date%] [LobbyWars] Player %player% has left PVP mode."
                        .replace("%player%", player.getName())
                        .replace("%date%", formatter.format(date)));
    }
    /**
     * Create a timer where every 2 ticks message on hotbar will appear
     * with the countdown to start PVP Mode.
     * When task finishes, it'll enable PVP mode for that player
     */
    public void startPlayerPvp(Player player) {
        int taskId = timerManager.startTimer(
                60,
                2,
                (duration) -> sendWarWarning(duration, player),
                () -> startWar(player),
                () -> ActionBarMessage.sendActionBarMessage(player, "")
        );
        setPvPTaskID(player, taskId);
    }
    /**
     * Update the hotbar countdown message
     */
    private void sendWarWarning(int duration, Player player) {
        String message = plugin.getConfigString("countdownMessage");
        float time = (float) duration / 20;
        message = message.replace("%timer%", String.valueOf(time));
        ActionBarMessage.sendActionBarMessage(player, message);
    }
    /**
     * Enable player's PVP, give him a proper gear
     */
    public void startWar(Player player) {
        String warMessage = plugin.getConfigString("joinPVPMessage");
        setPlayerCanDamage(player, true);
        removePvPTask(player);
        ActionBarMessage.sendActionBarMessage(player, warMessage);
        itemManager.giveDiamondGear(player);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss-SS");
        Date date = new Date();
        dbHelper.saveLogToRedis(
                "[%date%] [LobbyWars] Player %player% has entered PVP mode."
                        .replace("%player%", player.getName())
                        .replace("%date%", formatter.format(date)));
    }
    /**
     * Update players stats
     * @param damager - A killer
     * @param damaged - A victim
     */
    public void handlePlayerDeath(Player damager, Player damaged) {
        damaged.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        disablePlayerPvp(damaged);
        damaged.setHealth(damaged.getHealthScale());

        PlayerStatsCache.getStats(damager.getUniqueId()).incrementKills();
        PlayerStatsCache.getStats(damaged.getUniqueId()).incrementDeaths();

        String deathLogMessage = plugin.getConfigString("deathLogMessage")
                .replace("%killer%", damager.getDisplayName())
                .replace("%victim%", damaged.getDisplayName());
        Bukkit.broadcastMessage(deathLogMessage);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss-SS");
        Date date = new Date();
        dbHelper.saveLogToRedis(
                "[%date%] [LobbyWars] Player %victim% has been killed by %killer% in PVP mode."
                        .replace("%victim%", damaged.getName()
                        .replace("%killer%", damager.getName()
                        .replace("%date%", formatter.format(date)))));
    }
}
