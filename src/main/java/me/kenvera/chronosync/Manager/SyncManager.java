package me.kenvera.chronosync.Manager;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.chronosync.ChronoSync;
import me.kenvera.chronosync.Object.RedisPlayer;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncManager {
    private final ChronoSync plugin;
    Set<RedisPlayer> redisPlayerCache = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("ChronoSync").build());

    public SyncManager() {
        this.plugin = ChronoSync.getInstance();
    }

    public void addPlayer(Player player) {
        try (Jedis jedis = plugin.getRedisManager().getJedisPool().getResource()) {
            String server = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "null";
            String uuid = player.getUniqueId().toString();
            String domain = player.getVirtualHost().isPresent() ? player.getVirtualHost().get().getHostString() : "null";
            long ping = player.getPing();
            boolean onlineMode = player.isOnlineMode();
            String protocol = String.valueOf(player.getProtocolVersion().getVersionIntroducedIn());
            String clientBrand = player.getClientBrand();
            long joinTime = System.currentTimeMillis();
            if (!jedis.exists("group::" + player.getUsername())) {
                jedis.rpush("group::" + player.getUsername(), "default");
            }

            List<String> inheritedGroups = jedis.lrange("group::" + player.getUsername(), 0, -1);
            String ip = "null";

            InetSocketAddress address = player.getRemoteAddress();
            if (address == null) {
                ip = "null";
            }
//            System.out.println("address " + address);
//            System.out.println("ip address " + ip);

            assert address != null;
            InetAddress host = address.getAddress();
            if (host == null) {
                ip = "null";
            }
//            System.out.println("host " + host);
//            System.out.println("ip host " + ip);

            if (!ip.equalsIgnoreCase("null")) {
                ip = host.getHostAddress().replaceAll("/", "");
            }
//            System.out.println("ip " + ip);
//            System.out.println(host.getHostAddress());

            if (clientBrand == null) clientBrand = "null";
            Map<String, String> keyValues = Map.of(
                "Server", server,
                    "UUID", uuid,
                    "Domain", domain,
                    "Ping", String.valueOf(ping),
                    "Online Mode", String.valueOf(onlineMode),
                    "Protocol Version", protocol,
                    "Client Brand", clientBrand,
                    "Remote Address", ip,
                    "Join Time", String.valueOf(joinTime),
                    "Inherited Groups", inheritedGroups.toString()
            );

            jedis.hset("player::" + player.getUsername(), keyValues);
            jedis.expire("player::" + player.getUsername(), 5);
            jedis.sadd("playerlist", player.getUsername());
        }
    }

    public void updatePlayer(Player player, boolean resetJoinTime) {
        try (Jedis jedis = plugin.getRedisManager().getJedisPool().getResource()) {
            if (!jedis.exists("player::" + player.getUsername())) {
                addPlayer(player);
                return;
            }
            String server = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "null";
            String uuid = player.getUniqueId().toString();
            String virtualHost = player.getVirtualHost().isPresent() ? player.getVirtualHost().get().getHostString() : "null";
            long ping = player.getPing();
            boolean onlineMode = player.isOnlineMode();
            String protocol = String.valueOf(player.getProtocolVersion().getVersionIntroducedIn());
            String clientBrand = player.getClientBrand();
            long joinTime = System.currentTimeMillis();
            List<String> inheritedGroups = jedis.lrange("group::" + player.getUsername(), 0, -1);

            String ip = "null";

            InetSocketAddress address = player.getRemoteAddress();
            if (address == null) {
                ip = "null";
            }

            if (clientBrand == null) clientBrand = "null";
            InetAddress host = address.getAddress();
            if (host == null) {
                ip = "null";
            }

            if (!ip.equalsIgnoreCase("null")) {
                ip = host.getHostAddress().replaceAll("/", "");
            }

            assert clientBrand != null;

            if (resetJoinTime) {
                Map<String, String> keyValues = Map.of(
                        "Server", server,
                        "UUID", uuid,
                        "Virtual Host", virtualHost,
                        "Ping", String.valueOf(ping),
                        "Online Mode", String.valueOf(onlineMode),
                        "Protocol Version", protocol,
                        "Client Brand", clientBrand,
                        "Remote Address", ip,
                        "Join Time", String.valueOf(joinTime),
                        "Inherited Groups", inheritedGroups.toString()
                );

                jedis.hset("player::" + player.getUsername(), keyValues);
                jedis.expire("player::" + player.getUsername(), 5);
                jedis.sadd("playerlist", player.getUsername());

            } else {
                Map<String, String> keyValues = Map.of(
                        "Server", server,
                        "UUID", uuid,
                        "Virtual Host", virtualHost,
                        "Ping", String.valueOf(ping),
                        "Online Mode", String.valueOf(onlineMode),
                        "Protocol Version", protocol,
                        "Client Brand", clientBrand,
                        "Remote Address", ip,
                        "Inherited Groups", inheritedGroups.toString()
                );

                jedis.hset("player::" + player.getUsername(), keyValues);
                jedis.expire("player::" + player.getUsername(), 5);
                jedis.sadd("playerlist", player.getUsername());
            }
        }
    }

    public void removePlayer(Player player) {
        try (Jedis jedis = plugin.getRedisManager().getJedisPool().getResource()) {
            jedis.hdel("player::" + player.getUsername());
            jedis.srem("playerlist", player.getUsername());

            System.out.println("Remove Player Task: " + Thread.currentThread().getName());
        }
    }

    public boolean isOnline(Player player) {
        try (Jedis jedis = plugin.getRedisManager().getJedisPool().getResource()) {
            return jedis.exists("player::" + player.getUsername());
        }
    }

    public boolean isOnline(String username) {
        try (Jedis jedis = plugin.getRedisManager().getJedisPool().getResource()) {
            return jedis.exists("player::" + username);
        }
    }

    public void refreshCache() {
        Set<RedisPlayer> temp = ConcurrentHashMap.newKeySet();

        try (Jedis jedis = plugin.getRedisManager().getJedisPool().getResource()) {
            Set<String> storedDb = jedis.smembers("playerlist");
            if (storedDb == null) return;

            for (String keys : storedDb) {
                if (!jedis.exists("player::" + keys)) {
                    jedis.srem("playerlist", keys);
                    return;
                }

                JSONObject jsonObject = new JSONObject(jedis.hgetAll("player::" + keys));

                String server = jsonObject.optString("Server");
                String uuid = jsonObject.optString("UUID");
                String virtualHost = jsonObject.optString("Virtual Host");
                long ping = jsonObject.optLong("Ping");
                boolean onlineMode = jsonObject.optBoolean("Online Mode");
                String protocol = jsonObject.optString("Protocol Version");
                String clientBrand = jsonObject.optString("Client Brand");
                String remoteAddress = jsonObject.optString("Remote Address");
                long joinTime = jsonObject.optLong("Join Time");
                List<String> inheritedGroups = Collections.singletonList(jsonObject.optString("Inherited Groups"));

                temp.add(new RedisPlayer(keys, plugin.getDataManager().getString("proxyName"), server, uuid, virtualHost, ping, onlineMode, protocol, clientBrand, remoteAddress, joinTime, inheritedGroups));
            }
        }
        redisPlayerCache = temp;
    }

    public Optional<RedisPlayer> getPlayerData(String username) {
        for (RedisPlayer redisPlayer : redisPlayerCache) {
            if (redisPlayer.getUsername().equalsIgnoreCase(username)) {
                return Optional.of(redisPlayer);
            }
        }

        try (Jedis jedis = plugin.getRedisManager().getJedisPool().getResource()) {
            if (!jedis.exists("player::" + username)) {
                jedis.srem("playerlist", username);
                return Optional.empty();
            }

            JSONObject jsonObject = new JSONObject(jedis.hgetAll("player::" + username));

            String server = jsonObject.optString("Server");
            String uuid = jsonObject.optString("UUID");
            String virtualHost = jsonObject.optString("VirtualHost");
            long ping = jsonObject.optLong("Ping");
            boolean onlineMode = jsonObject.optBoolean("OnlineMode");
            String protocol = jsonObject.optString("ProtocolVersion");
            String clientBrand = jsonObject.optString("ClientBrand");
            String remoteAddress = jsonObject.optString("RemoteAddress");
            long joinTime = jsonObject.optLong("JoinTime");
            List<String> inheritedGroups = Collections.singletonList(jsonObject.optString("Inherited Groups"));

            RedisPlayer redisPlayer = new RedisPlayer(username, plugin.getDataManager().getString("proxyName"), server, uuid, virtualHost, ping, onlineMode, protocol, clientBrand, remoteAddress, joinTime, inheritedGroups);

            redisPlayerCache.add(redisPlayer);
            return Optional.of(redisPlayer);
        }
    }

    public int getPlayerCount() {
        return redisPlayerCache.size();
    }

    public Set<RedisPlayer> getPlayerDataCache() {
        return redisPlayerCache;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
