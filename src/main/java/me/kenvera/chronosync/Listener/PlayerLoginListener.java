package me.kenvera.chronosync.Listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import me.kenvera.chronosync.ChronoSync;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;

public class PlayerLoginListener {
    private final ChronoSync plugin;
    static final TextComponent ALREADY_LOGGED_IN = Component.text("Crazy").color(NamedTextColor.GREEN).append(Component.text("Network").color(NamedTextColor.WHITE))
            .append(Component.text("\nYour account is already logged in").color(NamedTextColor.RED))
            .append(Component.text("\n\nIf you think that this is a mistake, please wait 30 seconds...").color(NamedTextColor.GRAY))
            .append(Component.text("\nIf this issue persist, please contact us").color(NamedTextColor.GRAY));

    public PlayerLoginListener() {
        plugin = ChronoSync.getInstance();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLogin(LoginEvent event) {
        if (plugin.getSyncManager().isOnline(event.getPlayer())) {
            event.getPlayer().disconnect(ALREADY_LOGGED_IN);
            return;
        }

        if (!event.getResult().isAllowed()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            plugin.getSyncManager().addPlayer(event.getPlayer());
            plugin.getRedisManager().publish("login:" + event.getPlayer().getUniqueId().toString());
        }, plugin.getSyncManager().getExecutorService())
                .exceptionally(ex -> {
                    plugin.getLogger().error("Failed to log player login session " + event.getPlayer().getUsername());
                    ex.printStackTrace(System.err);
                    return null;
                });
    }
}
