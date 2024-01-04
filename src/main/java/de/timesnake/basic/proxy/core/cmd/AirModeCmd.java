/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;

public class AirModeCmd implements CommandListener {

  private final Code perm = Plugin.NETWORK.createPermssionCode("air");
  ;

  @Override
  public void onCommand(Sender sender, PluginCommand cmd,
      Arguments<Argument> args) {
    if (sender.hasPermission(this.perm)) {
      if (sender.isPlayer(false)) {
        User user = sender.getUser();
        if (user.isAirMode()) {
          user.setAirMode(false);
          user.getPlayer()
              .disconnect(Component.text("Back on the ground. Please rejoin"));
        } else {
          user.setAirMode(true);
          user.getPlayer().disconnect(Component.text("Up in the air. Please rejoin"));
        }
      } else {
        if (args.isLengthEquals(1, true)) {
          if (args.get(0).isPlayerDatabaseName(true)) {
            if (args.get(0).isPlayerName(false)) {
              sender.sendPluginMessage(Component.text("Only for offline players", ExTextColor.WARNING));
              return;
            }

            DbUser user = args.get(0).toDbUser();
            if (user.isAirMode()) {
              user.setAirMode(false);
              sender.sendPluginMessage(Component.text("Disabled air mode for user ",
                      ExTextColor.PERSONAL)
                  .append(Component.text(user.getName(), ExTextColor.VALUE)));
            } else {
              user.setAirMode(true);
              sender.sendPluginMessage(Component.text("Enabled air mode for user ",
                      ExTextColor.PERSONAL)
                  .append(Component.text(user.getName(), ExTextColor.VALUE)));
            }
          }
        }
      }
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
