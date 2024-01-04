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
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;

public class CleanupServersCmd implements CommandListener {

  private final Code perm = Plugin.SYSTEM.createPermssionCode("network.cleanup_servers");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!sender.hasPermission(this.perm)) {
      return;
    }

    if (!args.isLengthEquals(0, true)) {
      return;
    }

    for (Server server : Network.getServers()) {
      boolean result = Network.deleteServer(server.getName(), false);
      if (result) {
        sender.sendPluginMessage(Component.text("Deleted server ", ExTextColor.PERSONAL)
            .append(Component.text(server.getName(), ExTextColor.VALUE)));
      } else {
        sender.sendPluginMessage(
            Component.text("Can not delete server ", ExTextColor.WARNING)
                .append(Component.text(server.getName(), ExTextColor.VALUE)));
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
