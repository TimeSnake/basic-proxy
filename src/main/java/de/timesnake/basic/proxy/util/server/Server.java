package de.timesnake.basic.proxy.util.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.Status;

import java.nio.file.Path;
import java.time.Duration;

public abstract class Server extends BukkitConsole {

    protected DbServer database;
    protected int port;
    protected Type.Server<?> type;
    protected Status.Server status;
    protected Integer maxPlayers;

    protected Server(DbServer database, Path folderPath) {
        super(database.getName(), folderPath);
        this.database = database;
        this.port = database.getPort();
        this.type = Database.getServers().getServerType(this.port);
        this.status = database.getStatus();
        this.maxPlayers = database.getMaxPlayers();
    }

    public DbServer getDatabase() {
        return this.database;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public String getServerTask() {
        return this.type.getDatabaseValue();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.database.setMaxPlayers(maxPlayers);
        this.maxPlayers = maxPlayers;
    }

    public Status.Server getStatus() {
        return status;
    }

    public void setStatus(Status.Server serverStatus, boolean updateDatabase) {
        if (updateDatabase) {
            if (this.status != serverStatus) {
                this.database.setStatus(serverStatus);
                this.database.setOnlinePlayers(0);
            }
        }
        this.status = serverStatus;


        if (this.status == Status.Server.LAUNCHING) {
            Network.runTaskLater(() -> {
                if (status == Status.Server.LAUNCHING || status == Status.Server.OFFLINE) {
                    Network.printWarning(Plugin.NETWORK, "Failed to start server " + this.getName());
                    this.setStatus(Status.Server.OFFLINE, true);
                    return;
                }

                Network.runTaskLater(() -> {
                    if (status == Status.Server.LAUNCHING || status == Status.Server.OFFLINE
                            || status.equals(Status.Server.LOADING)) {
                        Network.printWarning(Plugin.NETWORK, "Failed to start server " + this.getName());
                        this.setStatus(Status.Server.OFFLINE, true);
                    }
                }, Duration.ofMinutes(3));
            }, Duration.ofSeconds(30));
        }

    }

    public Integer getOnlinePlayers() {
        return this.database.getOnlinePlayers();
    }

    public boolean start() {
        this.setStatus(Status.Server.LAUNCHING, true);
        return super.start();
    }

    public void updateStatus() {
        this.status = database.getStatus();
    }

    public Type.Server<?> getType() {
        return this.type;
    }

    public RegisteredServer getBungeeInfo() {
        return BasicProxy.getServer().getServer(this.name).get();
    }

}