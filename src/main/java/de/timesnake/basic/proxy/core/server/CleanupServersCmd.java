/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class CleanupServersCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.cleanup_servers");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);
    args.isLengthEqualsElseExit(0, true);

    for (Server server : Network.getServers()) {
      boolean result = Network.deleteServer(server.getName(), false);
      if (result) {
        sender.sendPluginTDMessage("§sDeleted server §v" + server.getName());
      } else {
        sender.sendPluginTDMessage("§sCan not delete server §v" + server.getName());
      }
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion();
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
