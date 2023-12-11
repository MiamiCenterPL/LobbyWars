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

import java.util.UUID;

public class MongoDBManager {
    LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    private MongoDatabase DB;
    private MongoClient mongoClient;
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }
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
            e.printStackTrace();
        }
    }

    public void updatePlayerStats(UUID uuid, String playerName, boolean dead, boolean kill) {
        if (!isDatabaseConnected()) return;
        MongoCollection<Document> playerStatsCollection = DB.getCollection("player_stats");

        // Sprawdź, czy dokument dla gracza już istnieje
        Bson filter = Filters.eq("UUID", uuid);
        Document existingDocument = playerStatsCollection.find(filter).first();

        if (existingDocument != null) {
            // Aktualizuj istniejący dokument
            int updatedKills = existingDocument.getInteger("kills", 0);
            int updatedDeaths = existingDocument.getInteger("deaths", 0);

            if (kill) {
                updatedKills += 1;
            }

            if (dead) {
                updatedDeaths += 1;
            }

            Bson update = new Document("$set", new Document("playerName", playerName)
                    .append("deaths", updatedDeaths)
                    .append("kills", updatedKills));

            playerStatsCollection.updateOne(filter, update);
        } else {
            Document newPlayerStats = new Document("UUID", uuid)
                    .append("playerName", playerName)
                    .append("deaths", dead ? 1 : 0)
                    .append("kills", kill ? 1 : 0);
            playerStatsCollection.insertOne(newPlayerStats);
        }
    }

    public int getPlayerKills(UUID uuid) {
        if (!isDatabaseConnected()) return 0;
        MongoCollection<Document> playerStatsCollection = DB.getCollection("player_stats");
        Bson filter = Filters.eq("UUID", uuid);

        Document playerStatsDocument = playerStatsCollection.find(filter).first();
        if (playerStatsDocument != null) {
            return playerStatsDocument.getInteger("kills", 0);
        } else {
            return 0;
        }
    }

    public int getPlayerDeaths(UUID uuid) {
        if (!isDatabaseConnected()) return 0;
        MongoCollection<Document> playerStatsCollection = DB.getCollection("player_stats");
        Bson filter = Filters.eq("UUID", uuid);

        Document playerStatsDocument = playerStatsCollection.find(filter).first();
        if (playerStatsDocument != null) {
            return playerStatsDocument.getInteger("deaths", 0);
        } else {
            return 0;
        }
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
