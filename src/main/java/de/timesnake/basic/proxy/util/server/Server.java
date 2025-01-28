/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.server.DbServer;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.UserSet;
import de.timesnake.library.network.NetworkServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public abstract class Server {

  protected final Logger logger = LogManager.getLogger("network.server");

  protected final String name;
  protected int port;
  protected final Path folderPath;

  protected DbServer database;
  protected ServerType type;
  protected Status.Server status;
  protected Integer maxPlayers;
  protected NetworkServer networkServer;

  protected UserSet<User> waitingUsers = new UserSet<>();

  protected ScheduledTask startTimeoutTask;

  protected Server(DbServer database, Path folderPath, NetworkServer networkServer) {
    this.name = database.getName();
    this.folderPath = folderPath;
    this.database = database;
    this.port = database.getPort();
    this.type = Database.getServers().getServerType(this.port);
    this.status = database.getStatus();
    this.maxPlayers = database.getMaxPlayers();
    this.networkServer = networkServer;
  }

  private String getStartScriptCommand() {
    return this.folderPath.toAbsolutePath() + "/start.sh -t " + this.getServerTask() +
           (Network.isServerDebuggingEnabled() ? " --debugging " +
                                                 (Network.DEBUGGING_PORT_OFFSET + this.getPort()) : "");
  }

  public void stop() {
    this.execute("stop");
  }

  public void execute(String cmd) {
    Network.getChannel().sendMessage(new ChannelServerMessage<>(this.getName(), MessageType.Server.COMMAND, cmd));
  }

  public String getName() {
    return name;
  }

  public Path getFolderPath() {
    return this.folderPath;
  }

  public DbServer getDatabase() {
    return this.database;
  }

  public Integer getPort() {
    return port;
  }

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

  public @NotNull Status.Server getStatus() {
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

  public boolean start() {
    this.setStatus(Status.Server.LAUNCHING, true);

    List<String> command = new ArrayList<>();

    if (Network.isTmuxEnabled()) {
      command.add("/bin/sh");
      command.add("-c");
      command.add("tmux new-window -n " + this.name + " -t " + Network.TMUX_SESSION_NAME + ": " + this.getStartScriptCommand());
    } else {
      command.add("konsole");
      command.add("--separate");
      command.add("--workdir " + this.folderPath.toAbsolutePath());
      command.add(" -e " + this.getStartScriptCommand());
    }

    try {
      Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
      Scanner sc = new Scanner(process.getErrorStream());
      while (sc.hasNextLine()) {
        this.logger.warn(sc.nextLine());
      }
      return true;
    } catch (IOException e) {
      this.logger.warn("Failed to start server {}: {}", this.name, e.getMessage());
      return false;
    }
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