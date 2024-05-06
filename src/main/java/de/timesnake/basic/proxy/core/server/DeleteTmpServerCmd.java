/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class DeleteTmpServerCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.delete_tmp");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);
    args.isLengthEqualsElseExit(1, true);

    String serverName = args.getString(0);
    boolean result = Network.deleteServer(serverName, false);

    if (result) {
      sender.sendPluginTDMessage("§sDeleted server §v" + serverName);
    } else {
      sender.sendPluginTDMessage("§sCan not delete server §v" + serverName);
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(Completion.ofServerNames());
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
