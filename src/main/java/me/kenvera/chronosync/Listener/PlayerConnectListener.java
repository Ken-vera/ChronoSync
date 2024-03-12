package me.kenvera.chronosync.Listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import me.kenvera.chronosync.ChronoSync;

import java.util.concurrent.CompletableFuture;

public class PlayerConnectListener {
    private final ChronoSync plugin;

    public PlayerConnectListener() {
        plugin = ChronoSync.getInstance();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onServerConnect(ServerPostConnectEvent event) {
        CompletableFuture.runAsync(() -> {
                    plugin.getSyncManager().updatePlayer(event.getPlayer(), true);
                    plugin.getRedisManager().publish("update:" + event.getPlayer().getUniqueId().toString());
                }, plugin.getSyncManager().getExecutorService())
                .exceptionally(ex -> {
                    plugin.getLogger().error("Failed to log player connect session " + event.getPlayer().getUsername());
                    ex.printStackTrace(System.err);
                    return null;
                });
    }
}
