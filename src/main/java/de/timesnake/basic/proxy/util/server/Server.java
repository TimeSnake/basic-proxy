/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.UserSet;
import de.timesnake.library.network.NetworkServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.Duration;

public abstract class Server extends BukkitServer {

  protected final Logger logger = LogManager.getLogger("network.server");

  protected DbServer database;
  protected int port;
  protected ServerType type;
  protected Status.Server status;
  protected Integer maxPlayers;
  protected NetworkServer networkServer;

  protected UserSet<User> waitingUsers = new UserSet<>();

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
    return this.type.getShortName();
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
          this.logger.warn("Failed to start server {}", this.getName());
          this.setStatus(Status.Server.OFFLINE, true);
          return;
        }

        this.startTimeoutTask = Network.runTaskLater(() -> {
          if (!status.isRunning()) {
            this.logger.warn("Failed to start server {}", this.getName());
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

  public ServerType getType() {
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