/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.*;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class ServerCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.server.cmd");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!sender.hasPermission(this.perm)) {
      return;
    }

    if (!args.isLengthHigherEquals(1, true)) {
      return;
    }

    if (args.get(0).equalsIgnoreCase("all")) {
      if (!args.isLengthHigherEquals(2, true)) {
        return;
      }

      if (args.get(1).equalsIgnoreCase("start")) {
        for (Server server : Network.getServers()) {
          if (server.getStatus().equals(Status.Server.OFFLINE)) {
            server.start();
          }
        }
      } else if (args.get(1).equalsIgnoreCase("stop")) {
        this.stopAllServers();
        sender.sendPluginTDMessage("§sAll servers stopped");
      } else {
        sender.sendPluginTDMessage("§wOnly commands §vstart §wand §vstop §w are allowed");
      }

    } else if (args.get(0).isServerName(true)) {
      if (!args.isLengthHigherEquals(2, true)) {
        sender.sendTDMessageCommandHelp("Execute command on server",
            "cmd <server> <cmd> [args]");
        return;
      }

      if (args.get(1).equalsIgnoreCase("start")) {
        this.startServer(sender, args);
        return;
      }

      Server server = args.get(0).toServer();
      String bukkitCmd = args.toMessage(1);
      server.execute(bukkitCmd);
      sender.sendPluginTDMessage("§sExecuted command on server §v" + server.getName() + "§s: §v" + bukkitCmd);
    }
  }

  public void startServer(Sender sender, Arguments<Argument> args) {
    if (Network.getServer(args.get(1).toLowerCase()) == null) {
      sender.sendPluginTDMessage("§wServer §v" + args.get(1).toLowerCase() + "§w doesn't exist!");
      return;
    }

    Server server = Network.getServer(args.get(1).toLowerCase());

    if (server.getStatus().equals(Status.Server.OFFLINE)) {
      sender.sendPluginTDMessage("§wServer §v" + args.get(1).toLowerCase() + "§w is already online!");
      return;
    }

    if (args.isLengthEquals(3, false) && args.get(3).isInt(true)) {
      server.setMaxPlayers(args.get(3).toInt());
    }

    this.startServer(sender, server);
  }

  public void startServer(Sender sender, Server server) {
    Network.runTaskAsync(() -> {
      boolean isStart = server.start();
      if (!isStart) {
        sender.sendPluginTDMessage("§wError while starting server §v" + server.getName());
        return;
      }

      sender.sendPluginTDMessage("§sStarted server §v" + server.getName());
    });
  }

  public void stopAllServers() {
    for (Server server : Network.getServers()) {
      if (!server.getStatus().equals(Status.Server.OFFLINE)) {
        server.stop();
      }
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(Completion.ofServerNames()
            .addArgument(new Completion("start", "stop", "password", "<cmd>")));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
