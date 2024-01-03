/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import net.kyori.adventure.text.Component;

public class DeleteTmpServerCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.delete_tmp");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!sender.hasPermission(this.perm)) {
      return;
    }

    if (!args.isLengthEquals(1, true)) {
      return;
    }

    String serverName = args.getString(0);

    boolean result = Network.deleteServer(serverName, false);

    if (result) {
      sender.sendPluginMessage(Component.text("Deleted server ", ExTextColor.PERSONAL)
          .append(Component.text(serverName, ExTextColor.VALUE)));
    } else {
      sender.sendPluginMessage(Component.text("Can not delete server ", ExTextColor.WARNING)
          .append(Component.text(serverName, ExTextColor.VALUE)));
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
