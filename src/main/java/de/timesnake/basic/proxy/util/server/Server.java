/*
 * basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.basic.proxy.util.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.extension.util.player.UserList;
import de.timesnake.library.network.NetworkServer;

import java.nio.file.Path;
import java.time.Duration;

public abstract class Server extends BukkitConsole {

    protected DbServer database;
    protected int port;
    protected Type.Server<?> type;
    protected Status.Server status;
    protected Integer maxPlayers;
    protected NetworkServer networkServer;

    protected UserList<User> waitingUsers = new UserList<>();

    protected ScheduledTask startTimeoutTask;

    protected Server(DbServer database, Path folderPath, NetworkServer networkServer) {
        super(database.getName(), folderPath);
        this.database = database;
        this.port = database.getPort();
        this.type = Database.getServers().getServerType(this.port);
        this.status = database.getStatus();
        this.maxPlayers = database.getMaxPlayers();
        this.networkServer = networkServer;
    }

    public DbServer getDatabase() {
        return this.database;
    }

    @Override
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

        if (this.status.isRunning() && this.startTimeoutTask != null) {
            this.startTimeoutTask.cancel();
            this.startTimeoutTask = null;
        }

        if (this.status == Status.Server.LAUNCHING) {
            this.startTimeoutTask = Network.runTaskLater(() -> {
                if (status == Status.Server.LAUNCHING || status == Status.Server.OFFLINE) {
                    Network.printWarning(Plugin.NETWORK, "Failed to start server " + this.getName());
                    this.setStatus(Status.Server.OFFLINE, true);
                    return;
                }

                this.startTimeoutTask = Network.runTaskLater(() -> {
                    if (!status.isRunning()) {
                        Network.printWarning(Plugin.NETWORK, "Failed to start server " + this.getName());
                        this.setStatus(Status.Server.OFFLINE, true);
                    }
                }, Duration.ofMinutes(3));
            }, Duration.ofSeconds(30));
        }

        this.connectWaitingUsers();
    }

    public Integer getOnlinePlayers() {
        return this.database.getOnlinePlayers();
    }

    @Override
    public boolean start() {
        this.setStatus(Status.Server.LAUNCHING, true);
        return super.start();
    }

    public void updateStatus() {
        this.status = database.getStatus();

        if (this.status.isRunning() && this.startTimeoutTask != null) {
            this.startTimeoutTask.cancel();
            this.startTimeoutTask = null;
        }
        this.connectWaitingUsers();
    }

    public Type.Server<?> getType() {
        return this.type;
    }

    public RegisteredServer getBungeeInfo() {
        return BasicProxy.getServer().getServer(this.name).get();
    }

    public NetworkServer getNetworkServer() {
        return networkServer;
    }

    private void connectWaitingUsers() {
        if (this.status.isRunning()) {
            this.waitingUsers.forEach(u -> u.connect(this.getBungeeInfo()));
            this.waitingUsers.clear();
        }
    }

    public void addWaitingUser(User user) {
        this.waitingUsers.add(user);
        this.connectWaitingUsers();
    }

    public void removeWaitingUser(User user) {
        this.waitingUsers.remove(user);
    }
}