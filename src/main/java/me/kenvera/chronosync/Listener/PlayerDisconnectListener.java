package me.kenvera.chronosync.Listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import me.kenvera.chronosync.ChronoSync;

import java.util.concurrent.CompletableFuture;

public class PlayerDisconnectListener {
    private final ChronoSync plugin;

    public PlayerDisconnectListener() {
        plugin = ChronoSync.getInstance();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerQuit(DisconnectEvent event) {
        CompletableFuture.runAsync(() -> {
                    plugin.getSyncManager().removePlayer(event.getPlayer());
                    plugin.getRedisManager().publish("disconnect:" + event.getPlayer().getUniqueId().toString());
                }, plugin.getSyncManager().getExecutorService())
                .exceptionally(ex -> {
                    plugin.getLogger().error("Failed to log player disconnect session " + event.getPlayer().getUsername());
                    ex.printStackTrace(System.err);
                    return null;
                });
    }
}
