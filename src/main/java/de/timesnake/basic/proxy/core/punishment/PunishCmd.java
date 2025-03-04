/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.punishment;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.PunishType;
import de.timesnake.library.basic.util.Punishment;
import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class PunishCmd implements CommandListener {

  private final Code punishPerm = PunishmentManager.PLUGIN.createPermssionCode("punish");
  private final Code mutePerm = PunishmentManager.PLUGIN.createPermssionCode("punish.netmute");
  private final Code kickPerm = PunishmentManager.PLUGIN.createPermssionCode("punish.netunmute");
  private final Code unmutePerm = PunishmentManager.PLUGIN.createPermssionCode("punish.kick");
  private final Code tempbanPerm = PunishmentManager.PLUGIN.createPermssionCode("punish.tempban");
  private final Code banPerm = PunishmentManager.PLUGIN.createPermssionCode("punish.ban");
  private final Code unbanPerm = PunishmentManager.PLUGIN.createPermssionCode("punish.unban");
  private final Code jailPerm = PunishmentManager.PLUGIN.createPermssionCode("punish.jail");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    args.isLengthHigherEqualsElseExit(1, true);
    args.get(0).assertElseExit(a -> a.isPlayerDatabaseName(true));
    DbUser user = args.get(0).toDbUser();

    switch (cmd.getName().toLowerCase()) {
      case "nettempmute", "tempmute" -> {
        sender.hasPermissionElseExit(this.mutePerm);

        if (!args.isLengthHigherEquals(3, true)) {
          sender.sendTDMessageCommandHelp("Temp-mute a player", "tempmute <player> <duration> <reason>");
        }

        Network.getPunishmentManager().tempMutePlayer(sender, user, new Punishment(user.getUniqueId(),
            PunishType.TEMP_MUTE, LocalDateTime.now(), parseDuration(args.get(1).getString()), sender.getName(),
            args.toMessage(2)));
      }
      case "netkick", "kick" -> {
        sender.hasPermissionElseExit(this.kickPerm);

        if (!args.isLengthHigherEquals(2, true)) {
          sender.sendTDMessageCommandHelp("Kick a player", "kick <player> <reason>");
          return;
        }

        Network.getPunishmentManager().kickPlayer(sender, args.get(0).toUser(), args.toMessage(1));
      }
      case "netunmute", "unmute" -> {
        sender.hasPermissionElseExit(this.unmutePerm);

        if (!args.isLengthHigherEquals(1, true)) {
          sender.sendTDMessageCommandHelp("Unmute a player", "unmute <player>");
          return;
        }

        Network.getPunishmentManager().unmutePlayer(sender, args.get(0).toDbUser());
      }
      case "nettempban", "nettmpban", "tempban", "tmpban" -> {
        sender.hasPermissionElseExit(this.tempbanPerm);

        if (!args.isLengthHigherEquals(3, true)) {
          sender.sendTDMessageCommandHelp("Temp-ban a player", "tempban <player> <durationInSec> <reason>");
          return;
        }

        Network.getPunishmentManager().tempBanPlayer(sender, user, new Punishment(user.getUniqueId(), PunishType.TEMP_BAN,
            LocalDateTime.now(), parseDuration(args.get(1).getString()), sender.getName(), args.toMessage(2)));
      }
      case "netban", "ban" -> {
        sender.hasPermissionElseExit(this.banPerm);

        if (!args.isLengthHigherEquals(2, true)) {
          sender.sendTDMessageCommandHelp("Ban a player", "ban <player> <reason>");
          return;
        }

        Network.getPunishmentManager().banPlayer(sender, user, new Punishment(user.getUniqueId(), PunishType.BAN,
            LocalDateTime.now(), null, sender.getName(), args.toMessage(1)));
      }
      case "netunban", "unban", "pardon", "netpardon" -> {
        sender.hasPermissionElseExit(this.unbanPerm);

        if (!args.isLengthEquals(1, true)) {
          sender.sendTDMessageCommandHelp("Unban a player", "unban <player>");
          return;
        }

        Network.getPunishmentManager().unbanPlayer(sender, args.get(0).toDbUser().getUniqueId());
      }
      case "netjail", "jail" -> {
        sender.hasPermissionElseExit(this.jailPerm);

        if (!args.isLengthHigherEquals(3, true)) {
          sender.sendTDMessageCommandHelp("Jail a player", "jail <player> <durationInSec> <reason>");
          return;
        }

        Network.getPunishmentManager().jailPlayer(sender, user, new Punishment(user.getUniqueId(), PunishType.JAIL,
            LocalDateTime.now(), parseDuration(args.get(1).getString()), sender.getName(), args.toMessage(2)));
      }
    }

  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.punishPerm)
        .addArgument(Completion.ofPlayerNames()
            .addArgument((sender, cmd, args) -> List.of("tmpban", "tempban", "jail", "tmpmute").contains(cmd),
                new Completion("10", "10*60", "5*60").allowAny()
                    .addArgument(new Completion("<reason>").allowAny()))
            .addArgument((sender, cmd, args) -> List.of("ban", "kick").contains(cmd),
                new Completion("<reason>").allowAny()));
  }

  @Override
  public String getPermission() {
    return this.punishPerm.getPermission();
  }

  private static Duration parseDuration(String s) {
    String[] plusParts = s.split("\\+");

    long result = 0;

    for (String plusPart : plusParts) {
      String[] multParts = plusPart.split("\\*");
      long multResult = 1;
      for (String multPart : multParts) {
        multResult *= Integer.parseInt(multPart);
      }
      result += multResult;
    }

    return Duration.ofSeconds(result);
  }
}
