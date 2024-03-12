package me.kenvera.chronosync.Object;

import com.velocitypowered.api.proxy.Player;
import me.kenvera.chronosync.ChronoSync;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RedisPlayer {
    private final ChronoSync plugin;
    String username;
    String proxy;
    String server;
    String uuid;
    String virtualhost;
    long ping;
    boolean onlineMode;
    String protocolVersion;
    String clientBrand;
    String remoteAddress;
    long joinTime;
    List<String> inheritedGroups;

    public RedisPlayer(String username, String proxy, String server, String uuid, String virtualhost, long ping, boolean onlineMode, String protocolVersion, String clientBrand, String remoteAddress, long joinTime, List<String> inheritedGroups) {
        plugin = ChronoSync.getInstance();
        this.username = username;
        this.proxy = proxy;
        this.server = server;
        this.uuid = uuid;
        this.virtualhost = virtualhost;
        this.ping = ping;
        this.onlineMode = onlineMode;
        this.protocolVersion = protocolVersion;
        this.clientBrand = clientBrand;
        this.remoteAddress = remoteAddress;
        this.joinTime = joinTime;
        this.inheritedGroups = inheritedGroups;
    }

    public String getUsername() {
        return username;
    }

    public String getProxy() {
        return proxy;
    }

    public String getServer() {
        return server;
    }

    public String getUuid() {
        return uuid;
    }

    public String getVirtualhost() {
        return virtualhost;
    }

    public long getPing() {
        return ping;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public String getClientBrand() {
        return clientBrand;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public List<String> getInheritedGroups() {
        return inheritedGroups;
    }

    public Optional<Player> getPlayer() {
        return plugin.getProxy().getPlayer(UUID.fromString(uuid));
    }
}

