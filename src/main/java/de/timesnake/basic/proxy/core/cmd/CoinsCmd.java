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

public class CoinsCmd implements CommandListener {

  private final Code perm = Plugin.TIME_COINS.createPermssionCode("timecoins.settings");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (sender.hasPermission(this.perm)) {
      if (args.isLengthEquals(3, true)) {
        if (args.get(0).isPlayerName(true)) {
          User user = args.get(0).toUser();
          if (args.get(2).isFloat(true)) {
            float coins = args.get(2).toFloat();
            switch (args.get(1).toLowerCase()) {
              case "add" -> {
                user.addCoins(coins);
                sender.sendPluginMessage(Component.text("Added ")
                    .append(Component.text(coins, ExTextColor.VALUE))
                    .append(Component.text(" timecoin(s) to ",
                        ExTextColor.PERSONAL))
                    .append(user.getChatNameComponent()));
              }
              case "remove" -> {
                user.removeCoins(coins);
                sender.sendPluginMessage(Component.text("Removed ")
                    .append(Component.text(coins, ExTextColor.VALUE))
                    .append(Component.text(" timecoin(s) from ",
                        ExTextColor.PERSONAL))
                    .append(user.getChatNameComponent()));
              }
              case "set" -> {
                user.setCoins(coins);
                sender.sendPluginMessage(Component.text("Set balance to ")
                    .append(Component.text(coins, ExTextColor.VALUE))
                    .append(Component.text(" timecoin(s) for ",
                        ExTextColor.PERSONAL))
                    .append(user.getChatNameComponent()));
              }
              case "reset" -> {
                user.setCoins(0);
                sender.sendPluginMessage(Component.text("Set balance to ")
                    .append(Component.text(coins, ExTextColor.VALUE))
                    .append(Component.text(" timecoin(s) for ",
                        ExTextColor.PERSONAL))
                    .append(user.getChatNameComponent()));
              }
            }
          }
        }
      }
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(Completion.ofPlayerNames()
            .addArgument(new Completion("add", "remove", "set", "reset")
                .addArgument(new Completion("0", "1", "10", "100"))));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
