/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.network;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class ServiceWorkCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.work.set");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (sender.hasPermission(this.perm)) {
      Network.setWork(!Network.isWork());
      sender.sendPluginTDMessage("§sService-Work: §v" + Network.isWork());
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
