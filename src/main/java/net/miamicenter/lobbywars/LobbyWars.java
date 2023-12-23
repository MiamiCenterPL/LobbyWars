package net.miamicenter.lobbywars;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class LobbyWars extends JavaPlugin {

    private FileConfiguration config;
    private static MongoDBManager mongoDBManager;
    private static RedisManager redisManager;
    @Override
    public void onDisable() {
        super.onDisable();

        PlayerStatsCache.saveAllToDB();

        mongoDBManager.disconnect();
        redisManager.disconnect();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(new EventListener(), this);
        mongoDBManager = MongoDBManager.getInstance();
        mongoDBManager.connect();

        redisManager = RedisManager.getInstance();
        this.getCommand("lobbywars").setExecutor(new CommandManager());
    }
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
