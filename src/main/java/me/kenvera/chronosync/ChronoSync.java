package me.kenvera.chronosync;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.chronosync.Command.FindCommand;
import me.kenvera.chronosync.Command.ListCommand;
import me.kenvera.chronosync.Command.PingCommand;
import me.kenvera.chronosync.Command.StaffListCommand;
import me.kenvera.chronosync.Listener.PlayerConnectListener;
import me.kenvera.chronosync.Listener.PlayerDisconnectListener;
import me.kenvera.chronosync.Listener.PlayerLoginListener;
import me.kenvera.chronosync.Listener.ServerPingListener;
import me.kenvera.chronosync.Manager.DataManager;
import me.kenvera.chronosync.Manager.RedisManager;
import me.kenvera.chronosync.Manager.SyncManager;
import me.kenvera.chronosync.Sync.Sync;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

@Plugin(
        id = "chronosync",
        name = "ChronoSync",
        version = "1.0",
        authors = "Kenvera"
)
public class ChronoSync {
    public static ChronoSync instance;
    private final ProxyServer proxyServer;
    private final CommandManager commandManager;
    private DataManager dataManager;
    private RedisManager redisManager;
    private SyncManager syncManager;
    private final Logger logger;

    @Inject
    public ChronoSync(ProxyServer proxy, Logger logger) {
        this.proxyServer = proxy;
        this.logger = logger;
        commandManager = proxy.getCommandManager();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;
        logger.info("Enabling ChronoSync...");

        dataManager = new DataManager();
        redisManager = new RedisManager();
        syncManager = new SyncManager();
        Sync sync = new Sync();

        proxyServer.getEventManager().register(this, new PlayerConnectListener());
        proxyServer.getEventManager().register(this, new PlayerDisconnectListener());
        proxyServer.getEventManager().register(this, new PlayerLoginListener());
        proxyServer.getEventManager().register(this, new ServerPingListener());

        registerCommand(commandManager, "stafflist", StaffListCommand.createBrigadierCommand(this), null, "sl");
        registerCommand(commandManager, "vlist", ListCommand.createBrigadierCommand(this), null);
        registerCommand(commandManager, "ping", PingCommand.createBrigadierCommand(this), null, "vping");
        registerCommand(commandManager, "find", FindCommand.createBrigadierCommand(this), null, "vfind");

        logger.info("ChronoSync Enabled!");

        getProxy().getScheduler().buildTask(this, sync).repeat(3, TimeUnit.SECONDS).schedule();
    }

    private void registerCommand(CommandManager commandManager, String label, BrigadierCommand brigadierCommand, SimpleCommand simpleCommand, String... aliases) {
        CommandMeta commandMeta = commandManager.metaBuilder(label).aliases(aliases).plugin(this).build();
        if (brigadierCommand != null) {
            commandManager.register(commandMeta, brigadierCommand);
        } else if (simpleCommand != null) {
            commandManager.register(commandMeta, simpleCommand);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public ProxyServer getProxy() {
        return proxyServer;
    }

    public LuckPerms getLuckPerms() {
        return LuckPermsProvider.get();
    }

    public static ChronoSync getInstance() {
        return instance;
    }
}
