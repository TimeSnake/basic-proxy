/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.permission;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;

public class PermissionTestCmd implements CommandListener {

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!sender.isPlayer(true)) {
      return;
    }

    if (!args.isLengthHigherEquals(1, true)) {
      return;
    }

    if (sender.hasPermission(args.getString(0))) {
      sender.sendPluginMessage(
          Component.text("You have the permission ", ExTextColor.PERSONAL)
              .append(Component.text(args.getString(0), ExTextColor.VALUE)));
    } else {
      sender.sendPluginMessage(
          Component.text("You have not the permission ", ExTextColor.PERSONAL)
              .append(Component.text(args.getString(0), ExTextColor.VALUE)));
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
