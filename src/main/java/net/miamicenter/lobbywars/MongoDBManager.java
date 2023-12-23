package net.miamicenter.lobbywars;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;

public class MongoDBManager {
    private static MongoDBManager instance;
    private final LobbyWars plugin;
    private static MongoDatabase DB;
    private static MongoClient mongoClient;
    private MongoDBManager() {
        this.plugin = LobbyWars.getPlugin(LobbyWars.class);
    }
    public static MongoDBManager getInstance(){
        if (instance == null) {
            instance = new MongoDBManager();
        }
        return instance;
    }
    public static MongoDatabase getDB() {
        return DB;
    }
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

        try {
            ServerApi serverApi = ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build();

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .codecRegistry(
                            CodecRegistries.fromRegistries(
                                    CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                                    MongoClientSettings.getDefaultCodecRegistry()
                            )
                    )
                    .serverApi(serverApi)
                    .build();

            // Create a new client and connect to the server
            mongoClient = MongoClients.create(settings);
            DB = mongoClient.getDatabase(db_name);

            // Rest of your connection code...

            System.out.println("Successfully connected to MongoDB!");

        } catch (MongoException e) {
            System.out.println("[ERROR] Could not connect to MongoDB...\n" + e);
        }
    }

    /**
    *   Check whether DB is connected/disconnected
     */
    public static boolean DBDisconnected() {
        return DB == null || mongoClient == null;
    }
    /**
     *  Try to create a DB collection if not existent.
     */
    public void createCollectionIfNotExists(String collectionName) {
        if (DBDisconnected() || !collectionExists(DB, collectionName)) return;
        try {
            DB.createCollection(collectionName);
            System.out.println("Collection '" + collectionName + "' created successfully.");
        } catch (MongoCommandException e) {
            System.err.println("No permission for creating collection: " + collectionName);
            System.err.println(e.getMessage());
        }
    }
    /**
     * This is used to check if given collection exists within DB
     */
    private boolean collectionExists(MongoDatabase database, String collectionName) {
        for (String existingCollection : database.listCollectionNames()) {
            if (existingCollection.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }
}
