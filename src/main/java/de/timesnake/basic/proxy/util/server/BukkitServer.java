/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public abstract class BukkitServer {

  protected final Logger logger;

  protected final String name;
  protected final Path folderPath;

  public BukkitServer(String name, Path folderPath) {
    this.name = name;
    this.folderPath = folderPath;

    this.logger = LogManager.getLogger("server." + this.name);
  }

  public boolean start() {
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

  public abstract Integer getPort();

  public abstract String getServerTask();
}
