/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.*;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

import static de.timesnake.library.chat.ExTextColor.*;
import static net.kyori.adventure.text.Component.text;

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
        sender.sendPluginMessage(text("All servers stopped", PERSONAL));
      } else {
        sender.sendPluginMessage(text("Only commands ", WARNING)
            .append(text("start ", VALUE))
            .append(text("and ", WARNING))
            .append(text("stop ", VALUE))
            .append(text(" are allowed on the all servers command", WARNING)));
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
      sender.sendPluginMessage(text("Executed command on server ", PERSONAL)
          .append(text(server.getName(), VALUE))
          .append(text(": ", PERSONAL))
          .append(text(bukkitCmd, VALUE)));
    }
  }

  public void startServer(Sender sender, Arguments<Argument> args) {
    if (Network.getServer(args.get(1).toLowerCase()) == null) {
      sender.sendPluginMessage(text("Server ", WARNING)
          .append(text(args.get(1).toLowerCase(), VALUE))
          .append(text(" doesn't exist!", WARNING)));
      return;
    }

    Server server = Network.getServer(args.get(1).toLowerCase());

    if (server.getStatus().equals(Status.Server.OFFLINE)) {
      sender.sendPluginMessage(text("Server ", WARNING)
          .append(text(args.get(1).toLowerCase(), VALUE))
          .append(text(" is already online!", WARNING)));
      return;
    }

    if (args.isLengthEquals(3, false) && args.get(3).isInt(true)) {
      server.setMaxPlayers(args.get(3).toInt());
    }

    this.handleServerCmd(sender, server);
  }

  public void handleServiceCommand(Sender sender, Arguments<Argument> args) {
    if (args.isLengthEquals(1, true)) {
      Server server = Network.getServer(args.get(0).getString());
      handleService(sender, args, server);
    } else {
      if (sender.isConsole(false)) {
        sender.sendMessageTooFewArguments();
        return;
      }

      Server server = sender.getUser().getServer();
      handleService(sender, args, server);
    }
  }

  private void handleService(Sender sender, Arguments<Argument> args, Server server) {
    if (server == null) {
      sender.sendMessageServerNameNotExist(args.get(0).getString());
      return;
    }

    if (server.getStatus().equals(Status.Server.SERVICE)) {
      sender.sendPluginMessage(text("Changed status of server ", PERSONAL)
          .append(text(server.getName(), VALUE))
          .append(text(" to service", PERSONAL)));
    } else {
      sender.sendPluginMessage(text("Changed status of server ", PERSONAL)
          .append(text(server.getName(), VALUE))
          .append(text(" to online", PERSONAL)));
    }
  }

  public void handleServerCmd(Sender sender, Server server) {
    Network.runTaskAsync(() -> {
      boolean isStart = server.start();
      if (!isStart) {
        sender.sendMessage(Chat.getSenderPlugin(Plugin.NETWORK)
            .append(text("Error while starting server ", WARNING))
            .append(text(server.getName(), VALUE)));
        return;
      }

      sender.sendMessage(Chat.getSenderPlugin(Plugin.NETWORK)
          .append(text("Started server ", PERSONAL))
          .append(text(server.getName(), VALUE)));
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
