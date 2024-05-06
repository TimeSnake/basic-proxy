/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.UUID;

public class UuidCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("network.uuid");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (!sender.hasPermission(this.perm)) {
      return;
    }

    if (!args.isLengthEquals(1, true)) {
      return;
    }

    Argument arg = args.get(0);

    if (arg.isUUID(false)) {
      UUID uuid = arg.toUUIDOrExit(true);
      DbUser dbUser = Database.getUsers().getUser(uuid);
      if (dbUser == null) {
        sender.sendPluginMessage(Component.text("Unknown user", ExTextColor.WARNING));
        return;
      }

      String name = dbUser.getName();

      sender.sendPluginMessage(Component.text(name, ExTextColor.PERSONAL, TextDecoration.UNDERLINED)
              .clickEvent(ClickEvent.copyToClipboard(name))
              .hoverEvent(HoverEvent.showText(Component.text("Click to copy"))));

    } else {
      String name = arg.getString();

      DbUser dbUser = Database.getUsers().getUser(name);
      if (dbUser == null) {
        sender.sendPluginMessage(Component.text("Unknown user", ExTextColor.WARNING));
        return;
      }

      UUID uuid = dbUser.getUniqueId();

      sender.sendPluginMessage(
          Component.text(uuid.toString(), ExTextColor.PERSONAL, TextDecoration.UNDERLINED)
              .clickEvent(ClickEvent.copyToClipboard(uuid.toString()))
              .hoverEvent(HoverEvent.showText(Component.text("Click to copy"))));
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(Completion.ofPlayerNames());
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
