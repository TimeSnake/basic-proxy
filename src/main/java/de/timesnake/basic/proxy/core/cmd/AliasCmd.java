/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.channel.util.message.ChannelUserMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class AliasCmd implements CommandListener {

  public static final Plugin PLUGIN = new Plugin("Alias", "PSA");

  private final Code perm = Plugin.NETWORK.createPermssionCode("alias");

  public static void setAlias(Sender sender, DbUser user, Argument type, Argument name) {
    String msg;
    switch (type.getString()) {
      case "info" -> {
        sender.sendPluginTDMessage("§sPlayer: §v" + user.getName());
        sender.sendPluginTDMessage("Prefix: §v" + user.getPrefix());
        sender.sendPluginTDMessage("Suffix: §v" + user.getSuffix());
        sender.sendPluginTDMessage("Nick: §v" + user.getNick());
      }
      case "prefix" -> {
        if (name == null) {
          user.setPrefix(null);
          msg = "§sReset prefix";
        } else {
          user.setPrefix(name.getString());
          msg = "§sUpdated prefix to §v" + name.getString();
        }
        sender.sendPluginTDMessage(msg + "§s of §v" + user.getName());
        Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.ALIAS));
      }
      case "suffix" -> {
        if (name == null) {
          user.setSuffix(null);
          msg = "§sReset suffix";
        } else {
          user.setSuffix(name.getString());
          msg = "§sUpdated suffix to §v" + name.getString();
        }
        sender.sendPluginTDMessage(msg + " §s of §v" + user.getName());
        Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.ALIAS));
      }
      case "nick" -> {
        if (name == null) {
          user.setNick(null);
          msg = "§sReset nick";
        } else {
          user.setNick(name.getString());
          msg = "§sUpdated nick to §v" + name.getString();
        }
        sender.sendPluginTDMessage(msg + "§s from §v" + user.getName());
        Network.getChannel().sendMessage(new ChannelUserMessage<>(user.getUniqueId(), MessageType.User.ALIAS));
      }
      default -> {
        sender.sendTDMessageCommandHelp("Set alias for player", "alias <player> <prefix/suffix/nick> [value]");
        sender.sendTDMessageCommandHelp("Get alias from player", "alias <player> info");
      }
    }
  }

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);

    if (args.isLengthHigherEquals(1, true)) {
      if (Database.getUsers().containsUser(args.get(0).getString())) {
        if (args.isLengthEquals(3, false)) {
          AliasCmd.setAlias(sender, args.get(0).toDbUser(), args.get(1), args.get(2));
        } else {
          AliasCmd.setAlias(sender, args.get(0).toDbUser(), args.get(1), null);
        }
      } else if (args.get(0).equalsIgnoreCase("help")) {
        sender.sendTDMessageCommandHelp("Set alias for player", "alias <player> <prefix/suffix/nick> [value]");
        sender.sendTDMessageCommandHelp("Get alias from player", "alias <player> info");
      } else if (sender.isPlayer(true)) {
        if (args.isLengthEquals(2, false)) {
          AliasCmd.setAlias(sender, sender.getDbUser(), args.get(0), args.get(1));
        } else {
          AliasCmd.setAlias(sender, sender.getDbUser(), args.get(0), null);
        }
      } else {
        sender.sendTDMessageCommandHelp("For help", "alias help");
      }
    } else {
      sender.sendTDMessageCommandHelp("For help", "alias help");
    }
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(Completion.ofPlayerNames()
            .addArgument(new Completion("info", "prefix", "suffix", "nick")));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
