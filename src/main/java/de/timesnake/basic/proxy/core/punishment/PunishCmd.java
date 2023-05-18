/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.punishment;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.List;

public class PunishCmd implements CommandListener<Sender, Argument> {

  private Code mutePerm;
  private Code kickPerm;
  private Code unmutePerm;
  private Code tempbanPerm;
  private Code banPerm;
  private Code unbanPerm;
  private Code jailPerm;

  @Override
  public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
      Arguments<Argument> args) {
    switch (cmd.getName().toLowerCase()) {
      case "netmute", "mute" -> {
        sender.hasPermissionElseExit(this.mutePerm);

        if (!args.isLengthHigherEquals(2, true)) {
          sender.sendTDMessageCommandHelp("Mute a player", "netmute <player> <reason>");
          return;
        }

        args.get(0).assertElseExit(a -> a.isPlayerDatabaseName(true));

        Network.getPunishmentManager()
            .mutePlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
      }
      case "netkick", "kick" -> {
        sender.hasPermissionElseExit(this.kickPerm);

        if (!args.isLengthHigherEquals(2, true)) {
          sender.sendTDMessageCommandHelp("Kick a player", "netkick <player> <reason>");
          return;
        }

        args.get(0).assertElseExit(a -> ((Argument) a).isPlayerName(true));

        Network.getPunishmentManager()
            .kickPlayer(sender, args.get(0).toUser(), args.toMessage(1));
      }
      case "netunmute", "unmute" -> {
        sender.hasPermissionElseExit(this.unmutePerm);

        if (!args.isLengthHigherEquals(1, true)) {
          sender.sendTDMessageCommandHelp("Unmute a player", "unmute <player>");
          return;
        }

        args.get(0).assertElseExit(a -> a.isPlayerDatabaseName(true));

        Network.getPunishmentManager().unmutePlayer(sender, args.get(0).toDbUser());
      }
      case "nettempban", "nettmpban", "tempban", "tmpban" -> {
        sender.hasPermissionElseExit(this.tempbanPerm);

        if (!args.isLengthHigherEquals(3, true)) {
          sender.sendTDMessageCommandHelp("Temp-ban a player", "nettempban <player> " +
              "<durationInSec> <reason>");
          return;
        }

        args.get(0).assertElseExit(a -> a.isPlayerDatabaseName(true));

        Network.getPunishmentManager().tempBanPlayer(sender, args.get(0).toDbUser(),
            args.get(1).getString(), args.toMessage(2));
      }
      case "netban", "ban" -> {
        sender.hasPermissionElseExit(this.banPerm);

        if (!args.isLengthHigherEquals(2, true)) {
          sender.sendTDMessageCommandHelp("Ban a player", "netban <player> <reason>");
          return;
        }

        args.get(0).assertElseExit(a -> a.isPlayerDatabaseName(true));

        Network.getPunishmentManager()
            .banPlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
      }
      case "netunban", "unban", "pardon", "netpardon" -> {
        sender.hasPermissionElseExit(this.unbanPerm);

        if (!args.isLengthEquals(1, true)) {
          sender.sendTDMessageCommandHelp("Unban a player", "unban <player>");
          return;
        }

        args.get(0).assertElseExit(a -> a.isPlayerDatabaseName(true));

        Network.getPunishmentManager()
            .unbanPlayer(sender, args.get(0).toDbUser().getUniqueId());
      }
      case "netjail", "jail" -> {
        sender.hasPermissionElseExit(this.jailPerm);

        if (!args.isLengthHigherEquals(3, true)) {
          sender.sendTDMessageCommandHelp("Jail a player",
              "jail <player> <durationInSec> <reason>");
          return;
        }

        args.get(0).assertElseExit(a -> a.isPlayerDatabaseName(true));

        Network.getPunishmentManager()
            .jailPlayer(sender, args.get(0).toDbUser(), args.get(1).getString(),
                args.toMessage(2));
      }
    }

  }

  @Override
  public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
      Arguments<Argument> args) {
    int length = args.getLength();
    if (length == 1) {
      return Network.getCommandManager().getPlayerNames();
    }

    String arg0 = args.getString(0);

    if (length == 2 && (arg0.equalsIgnoreCase("tempban")
        || arg0.equalsIgnoreCase("tmpban")
        || arg0.equalsIgnoreCase("nettempban")
        || arg0.equalsIgnoreCase("nettmpban")
        || arg0.equalsIgnoreCase("jail")
        || arg0.equalsIgnoreCase("netjail"))) {
      return List.of("10", "10*60", "5*60");
    }
    return null;
  }

  @Override
  public void loadCodes(Plugin plugin) {
    this.mutePerm = plugin.createPermssionCode("punish.netmute");
    this.unmutePerm = plugin.createPermssionCode("punish.netunmute");
    this.kickPerm = plugin.createPermssionCode("punish.kick");
    this.tempbanPerm = plugin.createPermssionCode("punish.tempban");
    this.banPerm = plugin.createPermssionCode("punish.ban");
    this.unbanPerm = plugin.createPermssionCode("punish.unban");
    this.jailPerm = plugin.createPermssionCode("punish.jail");
  }
}
