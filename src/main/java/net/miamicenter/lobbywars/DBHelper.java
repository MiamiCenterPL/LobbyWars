package net.miamicenter.lobbywars;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class DBHelper {
    private final LobbyWars plugin;
    private static DBHelper instance;
    private static RedisManager redisManager;
    private DBHelper() {
        plugin = LobbyWars.getPlugin(LobbyWars.class);
        redisManager = RedisManager.getInstance();
    }
    public static DBHelper getInstance() {
        if (instance == null) {
            instance = new DBHelper();
        }
        return instance;
    }
    //Shared functions;
    /**
     * Generic Async task function
     *
     * @param executeFunction - A function that will be executed asynchronously,
     *                        although you won't be able to get function's result.
     *                        Used for function that returns void
     */

    private void asyncDBAccess(Runnable executeFunction) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, executeFunction);
    }
    /**
     * Generic Async task function
     *
     * @param executeFunction - A function that will be executed asynchronously,
     * @param onCallback     - (Optional) A function that will be executed as a result function,
     *                       this will also get the result from the executeFunction,
     * @param <T>            Type of the executeFunction's result.
     * @param <P>            Type of executeFunction's parameters
     */
    private <T, P> void asyncDBAccess(Supplier<T> executeFunction, BiConsumer<T, P> onCallback, P params) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            T result = executeFunction.get();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    onCallback.accept(result, params);
                } catch (ClassCastException e) {
                    System.out.println("[ERROR] Could not pass result, params from executeFunction to onCallback");
                }
            });
        });
    }
    //MongoDB;
    /**
     *   Generic asynchronous function to update player nickname in DB,
     *   and add player stats to the Cache.
     *   @param player - Player instance.
     */
    public void getPlayerStats(Player player) {
        if (MongoDBManager.DBDisconnected()) {
            System.out.println("DB not connected!");
            return;
        }
        asyncDBAccess(() -> getPlayerStatsAsync(player));
    }
    /**
     *   Asynchronous function, please use getPlayerStats to execute.
     */
    private void getPlayerStatsAsync(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        String collectionName = plugin.getConfigString("dbCollectionName");
        MongoCollection<Document> playerStatsCollection = MongoDBManager.getDB().getCollection(collectionName);
        Bson filter = Filters.eq("UUID", uuid);
        Document existingDocument = playerStatsCollection.find(filter).first();
        PlayerStats stats;
        if (existingDocument != null) {
            int kills = existingDocument.getInteger("kills", 0);
            int deaths = existingDocument.getInteger("deaths", 0);
            String DBName = existingDocument.getString("playerName");
            stats = new PlayerStats(uuid, kills, deaths);

            if (!DBName.equals(name)) {
                Bson update = new Document("$set", new Document("playerName", name));
                playerStatsCollection.updateOne(filter, update);
            }
        } else {
            stats = new PlayerStats(uuid, 0, 0);
            Document newPlayerStats = new Document("UUID", uuid)
                    .append("playerName", name)
                    .append("deaths", 0)
                    .append("kills", 0);
            playerStatsCollection.insertOne(newPlayerStats);
        }
        PlayerStatsCache.putStats(stats);
    }
    /**
     * Update player's statistics on DB.
     *
     * @param uuid           - Player's Unique identifier,
     * @param record         - Player's statistics from Cache
     */
    public void updatePlayerStats(UUID uuid, PlayerStats record) {
        if (MongoDBManager.DBDisconnected()) return;
        asyncDBAccess(() -> updatePlayerStatsAsync(uuid, record));
    }
    /**
     * Please use updatePlayerStats function instead,
     * this function will be called asynchronously when
     * executed by updatePlayerStats function.
     */
    private void updatePlayerStatsAsync(UUID uuid, PlayerStats record) {
        String collectionName = plugin.getConfigString("dbCollectionName");
        MongoCollection<Document> playerStatsCollection = MongoDBManager.getDB().getCollection(collectionName);
        Bson filter = Filters.eq("UUID", uuid);
        Document existingDocument = playerStatsCollection.find(filter).first();

        if (existingDocument != null) {
            Bson update = new Document("$set", new Document("deaths", record.getDeaths())
                    .append("kills", record.getKills()));

            playerStatsCollection.updateOne(filter, update);
        } else {
            Document newPlayerStats = new Document("UUID", uuid)
                    .append("deaths", record.getDeaths())
                    .append("kills", record.getKills());
            playerStatsCollection.insertOne(newPlayerStats);
        }
    }
    //Redis:
    public void saveLogToRedis(String logMessage) {
        asyncDBAccess(() -> saveLogsAsync(logMessage));
    }
    private static void saveLogsAsync(String logMessage) {
        //SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss-SS");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        Date date = new Date();
        String logKey = "logs:" + formatter.format(date);
        if (redisManager.connect()) {
            RedisManager.getJedis().lpush(logKey, logMessage);
            redisManager.disconnect();
        }
    }
}
