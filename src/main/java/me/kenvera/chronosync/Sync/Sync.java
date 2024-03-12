package me.kenvera.chronosync.Sync;

import com.velocitypowered.api.proxy.Player;
import me.kenvera.chronosync.ChronoSync;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Sync implements Runnable{
    private final ChronoSync plugin;

    public Sync() {
        plugin = ChronoSync.getInstance();
    }

    @Override
    public void run() {
        CompletableFuture.runAsync(() -> {
                    Collection<Player> players = plugin.getProxy().getAllPlayers();
                    for (Player player : players)
                    {
                            plugin.getSyncManager().updatePlayer(player, false);
                    }
                    plugin.getSyncManager().refreshCache();
                }, plugin.getSyncManager().getExecutorService())
                .exceptionally(ex -> {
                    plugin.getLogger().error("Failed to run async method!");
                    ex.printStackTrace(System.err);
                    return null;
                });
    }
}
