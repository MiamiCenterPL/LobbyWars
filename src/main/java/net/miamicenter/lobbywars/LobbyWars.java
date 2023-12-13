package net.miamicenter.lobbywars;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.util.ArrayList;

public class LobbyWars extends JavaPlugin {
    private FileConfiguration config;
    private ItemManager itemManager;
    private MongoDBManager mongoDBManager;
    private TimerManager timerManager;
    private PvPManager pvpManager;
    @Override
    public void onDisable() {
        super.onDisable();
        mongoDBManager.disconnect();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        saveDefaultConfig();
        config = getConfig();
        itemManager = new ItemManager();
        timerManager = new TimerManager();
        pvpManager = new PvPManager();
        mongoDBManager = new MongoDBManager();

        getServer().getPluginManager().registerEvents(new EventListener(), this);

        mongoDBManager.connect();

        this.getCommand("lobbywars").setExecutor(new CommandManager());

        mongoDBManager.createCollectionIfNotExists("player_stats");
    }

    public ItemManager getItemManager() {
        return itemManager;
    }
    public TimerManager getTimerManager() { return timerManager; }
    public MongoDBManager getDBManager(){
        return mongoDBManager;
    }
    public PvPManager getPvPManager() {return pvpManager;}
    public String getConfigString(String var) {
        if (config.getString(var) == null) return "";
        return ChatColor.translateAlternateColorCodes('&', config.getString(var));
    }

    public ArrayList<String> getConfigList(String var) {
        ArrayList<String> colored_strings = new ArrayList<>();
        for (String loreLine : config.getStringList(var)) {
            colored_strings.add(ChatColor.translateAlternateColorCodes('&', loreLine));
        }
        return colored_strings;
    }
}
