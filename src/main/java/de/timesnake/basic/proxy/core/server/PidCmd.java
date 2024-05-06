/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.permissions.Permission;

public class PidCmd implements CommandListener {

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.isConsoleElseExit(true);
    sender.sendPluginTDMessage("§sPID: §v" + ProcessHandle.current().pid());
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion();
  }

  @Override
  public String getPermission() {
    return Permission.CONSOLE_PERM;
  }
}
