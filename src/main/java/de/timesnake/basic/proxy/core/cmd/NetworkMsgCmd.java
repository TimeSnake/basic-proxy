/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import net.kyori.adventure.text.Component;

public class NetworkMsgCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.message");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd,
      Arguments<Argument> args) {
    if (sender.hasPermission(this.perm)) {
      if (sender.isPlayer(true)) {
        if (args.isLengthEquals(0, true)) {
          User user = sender.getUser();
          user.setListeningNetworkMessages(!user.isListeningNetworkMessages());
          if (user.isListeningNetworkMessages()) {
            sender.sendPluginMessage(
                Component.text("Enabled network messages", ExTextColor.PERSONAL));
          } else {
            sender.sendPluginMessage(
                Component.text("Disabled network messages", ExTextColor.PERSONAL));
          }
        }
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
