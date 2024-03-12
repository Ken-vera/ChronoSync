package me.kenvera.chronosync.Listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import me.kenvera.chronosync.ChronoSync;

public class ServerPingListener {
    private final ChronoSync plugin;

    public ServerPingListener() {
        plugin = ChronoSync.getInstance();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerPing(ProxyPingEvent event) {
        event.setPing(event.getPing().asBuilder().onlinePlayers(plugin.getSyncManager().getPlayerCount()).build());
    }
}
