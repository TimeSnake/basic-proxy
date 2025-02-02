/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class ServerDebugCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.debug");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);
    Network.setServerDebugging(!Network.isServerDebuggingEnabled());
    sender.sendPluginTDMessage("§s" + (Network.isServerDebuggingEnabled() ? "Enabled" : "Disabled") + " server " +
                               "debugging");
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm);
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
