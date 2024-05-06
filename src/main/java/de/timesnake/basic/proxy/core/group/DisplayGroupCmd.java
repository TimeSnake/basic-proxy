/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

import java.util.UUID;

public class DisplayGroupCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("chat.display_group");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!sender.hasPermission(this.perm)) {
      return;
    }

    if (!args.isLengthHigherEquals(2, true)) {
      return;
    }

    if (!args.get(0).isPlayerDatabaseName(true)) {
      return;
    }

    if (!args.isLengthEquals(3, true)) {
      return;
    }

    String groupName = args.getString(2).toLowerCase();
    DisplayGroup group = Network.getGroupManager().getDisplayGroup(groupName);

    if (group == null) {
      sender.sendPluginTDMessage("§wDisplay group §v" + groupName + "§w not exists");
      return;
    }

    DbUser user = args.get(0).toDbUser();
    String userName = user.getName();
    UUID uuid = user.getUniqueId();
    String action = args.getString(1).toLowerCase();

    switch (action) {
      case "add" -> {
        if (user.getDisplayGroupNames().contains(groupName)) {
          sender.sendPluginTDMessage("§v" + userName + "§w is already member of display group §v" + groupName);
          return;
        }

        user.addDisplayGroup(groupName);
        sender.sendPluginTDMessage("§sAdded §v" + userName + "§s to display group §v" + groupName);
      }
      case "remove" -> {
        if (!user.getDisplayGroupNames().contains(groupName)) {
          sender.sendPluginTDMessage("§v" + userName + "§w is not a member of display group §v" + groupName);
          return;
        }

        user.removeDisplayGroup(groupName);
        sender.sendPluginTDMessage("§sRemoved §v" + userName + "§s from display group §v" + groupName);
      }
      default -> {
        return;
      }
    }

    if (Network.isUserOnline(uuid)) {
      Network.getUser(uuid).updateDisplayGroup();
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(Completion.ofPlayerNames()
            .addArgument(new Completion("add", "remove")
                .addArgument(Completion.ofDisplayGroupNames())));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
