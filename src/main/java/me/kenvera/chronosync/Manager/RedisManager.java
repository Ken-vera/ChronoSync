package me.kenvera.chronosync.Manager;

import me.kenvera.chronosync.ChronoSync;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisManager {
    private final ChronoSync plugin;
    private final JedisPool jedisPool;
    private final String channelName;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public RedisManager() {
        this.plugin = ChronoSync.getInstance();
        String host = plugin.getDataManager().getString("redis.host");
        int port = plugin.getDataManager().getInt("redis.port");
        String password = plugin.getDataManager().getString("redis.password");
        String user = plugin.getDataManager().getString("redis.user", "default");
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(100);
        jedisPoolConfig.setMaxIdle(2);
        jedisPoolConfig.setMinIdle(1);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        jedisPoolConfig.setTestWhileIdle(true);
        jedisPoolConfig.setNumTestsPerEvictionRun(-1);
        jedisPoolConfig.setBlockWhenExhausted(false);
        this.jedisPool = new JedisPool(jedisPoolConfig, host, port, 5000, user, password, 0);
        channelName = plugin.getDataManager().getString("redis.channel", "chronosync");
    }

    public void disconnect() {
        plugin.getLogger().info("Disconnecting from Redis...");
        jedisPool.close();
        plugin.getLogger().info("Disconnected from Redis!");
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void publish(String message) {
        executorService.execute(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channelName, message);
            }
        });
    }


}
