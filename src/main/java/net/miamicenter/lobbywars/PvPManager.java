package net.miamicenter.lobbywars;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class PvPManager {
    LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    ItemManager itemManager = plugin.getItemManager();
    TimerManager timerManager = plugin.getTimerManager();
    private final NamespacedKey triggerPvpTaskKey = CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin);
    private final NamespacedKey canPvpKey = CustomAttributes.CAN_PVP.getNamespacedKey(plugin);
    //Booleans;
    public boolean isTriggerPvpTaskRunning(Player player) {
        return player.getPersistentDataContainer().has(triggerPvpTaskKey, PersistentDataType.INTEGER);
    }

    public int triggerPvPTaskID(Player player) {
        return player.getPersistentDataContainer().get(triggerPvpTaskKey, PersistentDataType.INTEGER);
    }

    public void removePvPTask(Player player) {
        timerManager.stopTimer(triggerPvPTaskID(player));
        player.getPersistentDataContainer().remove(triggerPvpTaskKey);
    }
    public void setPvPTaskID(Player player, int taskID){
        player.getPersistentDataContainer().set(triggerPvpTaskKey, PersistentDataType.INTEGER, taskID);
    }
    public void setPlayerCanDamage(Player player, boolean var){
        player.getPersistentDataContainer().set(canPvpKey, PersistentDataType.BOOLEAN, var);
    }

    public boolean canDamage(Player player){
        if (player.getPersistentDataContainer().has(canPvpKey, PersistentDataType.BOOLEAN)){
            return player.getPersistentDataContainer().get(canPvpKey, PersistentDataType.BOOLEAN);
        } else {
            return false;
        }
    }

    public void disablePlayerPvp(Player player) {
        NamespacedKey canPvp = CustomAttributes.CAN_PVP.getNamespacedKey(plugin);
        player.getPersistentDataContainer().set(canPvp, PersistentDataType.BOOLEAN, false);
        String quitMessage = plugin.getConfigString("quitPVPMessage");
        ActionBarMessage.sendActionBarMessage(player, quitMessage);
        itemManager.clearArmour(player);
        itemManager.givePvPItem(player);
    }

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
    private void sendWarWarning(int duration, Player player) {
        String message = plugin.getConfigString("countdownMessage");
        float time = (float) duration / 20;
        message = message.replace("%timer%", String.valueOf(time));
        ActionBarMessage.sendActionBarMessage(player, message);
    }
    public void startWar(Player player) {
        String warMessage = plugin.getConfigString("joinPVPMessage");
        setPlayerCanDamage(player, true);
        removePvPTask(player);
        ActionBarMessage.sendActionBarMessage(player, warMessage);
        plugin.getItemManager().giveDiamondGear(player);
    }
    public void handlePlayerDeath(Player damager, Player damaged) {
        damaged.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        disablePlayerPvp(damaged);
        damaged.setHealth(damaged.getHealthScale());

        plugin.getDBManager().updatePlayerStats(damager.getUniqueId(), false, true);
        plugin.getDBManager().updatePlayerStats(damaged.getUniqueId(), true, false);

        String deathLogMessage = plugin.getConfigString("deathLogMessage")
                .replace("%killer%", damager.getDisplayName())
                .replace("%victim%", damaged.getDisplayName());
        Bukkit.broadcastMessage(deathLogMessage);
    }
}
