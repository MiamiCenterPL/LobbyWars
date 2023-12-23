package net.miamicenter.lobbywars;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisManager {
    private static RedisManager instance;
    private static Jedis jedis;
    private final LobbyWars plugin;
    private RedisManager() {
        this.plugin = LobbyWars.getPlugin(LobbyWars.class);
    }
    public static RedisManager getInstance() {
        if (instance == null) {
            instance = new RedisManager();
        }
        return instance;
    }
    public boolean connect() {
        String IP = plugin.getConfigString("redisIP");
        int port = Integer.parseInt(plugin.getConfigString("redisPort"));
        String password = plugin.getConfigString("redisPassword");

        try (JedisPool pool = new JedisPool(IP, port)) {
            jedis = pool.getResource();
            if (!password.isEmpty()) jedis.auth(password);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
    public static Jedis getJedis(){
        return jedis;
    }
    public void disconnect() {
        if (jedis != null) {
            jedis.close();
            System.out.println("[LobbyWars] Redis disconnected.");
        }
    }
}
