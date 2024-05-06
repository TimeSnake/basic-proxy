/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.permission;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class PermissionTestCmd implements CommandListener {

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.isPlayerElseExit(true);
    args.isLengthEqualsElseExit(1, true);

    if (sender.hasPermission(args.getString(0))) {
      sender.sendPluginTDMessage("§sYou have permission §v" + args.getString(0));
    } else {
      sender.sendPluginTDMessage("§sYou do not have permission §v" + args.getString(0));
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion();
  }

  @Override
  public String getPermission() {
    return null;
  }
}
