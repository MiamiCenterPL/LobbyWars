package net.miamicenter.lobbywars;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MongoDBManager {
    LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    private MongoDatabase DB;
    private MongoClient mongoClient;
    /**
     * A function that will disconnect from MongoDB
     */
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }
    /**
     * A function that will try to connect to the Mongo Data Base
     * that is set in the config.yml
     */
    public void connect() {
        String connectionString = plugin.getConfigString("dbConnectionString");
        String db_name = plugin.getConfigString("dbName");
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(
                        CodecRegistries.fromRegistries(
                                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),  // Tutaj ustaw standardowe kodowanie UUID
                                MongoClientSettings.getDefaultCodecRegistry()
                        )
                )
                .serverApi(serverApi)
                .build();


        // Create a new client and connect to the server
        mongoClient = MongoClients.create(settings);
        DB = mongoClient.getDatabase(db_name);

        try {
            // Send a ping to confirm a successful connection
            DB.runCommand(new Document("ping", 1));
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
        } catch (MongoException e) {
            System.out.println("[ERROR] Could not connect to MongoDB!");
        }
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
     * Update player's death and kill counts.
     *
     * @param uuid           - Player's Unique identifier,
     * @param dead           - If true, will increase player death count by 1
     * @param kill            - If true, will increase player kill count by 1
     */
    public void updatePlayerStats(UUID uuid, boolean dead, boolean kill) {
        if (!isDatabaseConnected()) return;
        //Make async task:
        asyncDBAccess(() -> updatePlayerStatsAsync(uuid, dead, kill));
    }
    /**
     * Please use updatePlayerStats function instead,
     * this function will be called asynchronously when
     * executed by updatePlayerStats function.
     */
    private void updatePlayerStatsAsync(UUID uuid, boolean dead, boolean kill) {
        MongoCollection<Document> playerStatsCollection = DB.getCollection("player_stats");
        Bson filter = Filters.eq("UUID", uuid);
        Document existingDocument = playerStatsCollection.find(filter).first();

        if (existingDocument != null) {
            int updatedKills = existingDocument.getInteger("kills", 0);
            int updatedDeaths = existingDocument.getInteger("deaths", 0);

            if (kill) {
                updatedKills += 1;
            }

            if (dead) {
                updatedDeaths += 1;
            }

            Bson update = new Document("$set", new Document("deaths", updatedDeaths)
                    .append("kills", updatedKills));

            playerStatsCollection.updateOne(filter, update);
        } else {
            Document newPlayerStats = new Document("UUID", uuid)
                    .append("deaths", dead ? 1 : 0)
                    .append("kills", kill ? 1 : 0);
            playerStatsCollection.insertOne(newPlayerStats);
        }
    }
    /**
     * Asynchronous function to fetch Player kills from DB
     * Later it will send that value to player.
     *
     * @param uuid - Player's UUID
     */
    public void getPlayerKills(UUID uuid) {
        if (!isDatabaseConnected()) return;
        asyncDBAccess(() -> getPlayerStatAsync(uuid, "kills"), this::sendPlayerKillCount, uuid);
    }
    /**
     * This simply sends player his kill count value
     *
     * @param kills - Player's kill counter
     * @param uuid - Player's UUID
     */
    private void sendPlayerKillCount(int kills, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        String msg = plugin.getConfigString("killCounterMessage")
            .replace("%kills%", String.valueOf(kills));
        player.sendMessage(msg);
    }

    public void getPlayerDeaths(UUID uuid) {
        if (!isDatabaseConnected()) return;
        asyncDBAccess(() -> getPlayerStatAsync(uuid, "deaths"), this::sendPlayerDeathsCount, uuid);
    }

    private int getPlayerStatAsync(UUID uuid, String var) {
        String collectionName = plugin.getConfigString("dbCollectionName");
        MongoCollection<Document> playerStatsCollection = DB.getCollection(collectionName);
        Bson filter = Filters.eq("UUID", uuid);

        Document playerStatsDocument = playerStatsCollection.find(filter).first();
        if (playerStatsDocument != null) {
            return playerStatsDocument.getInteger(var, 0);
        } else {
            return 0;
        }
    }
    private void sendPlayerDeathsCount(int deaths, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        String msg = plugin.getConfigString("deathCounterMessage").replace("%deaths%", String.valueOf(deaths));
        player.sendMessage(msg);
    }
    public void createCollectionIfNotExists(String collectionName) {
        if (isDatabaseConnected()){
            if (!collectionExists(DB, collectionName)) {
                try {
                    DB.createCollection(collectionName);
                    System.out.println("Collection '" + collectionName + "' created successfully.");
                } catch (MongoCommandException e) {
                    System.err.println("No permission for creating collection: " + collectionName);
                }
            }
        }
    }
    private boolean isDatabaseConnected() {
        return DB != null;
    }

    private boolean collectionExists(MongoDatabase database, String collectionName) {
        for (String existingCollection : database.listCollectionNames()) {
            if (existingCollection.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }
}
