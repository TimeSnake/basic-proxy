package de.timesnake.basic.proxy.core.user;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.channel.api.message.ChannelUserMessage;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.cmd.Arguments;
import de.timesnake.library.basic.util.cmd.CommandListener;
import de.timesnake.library.basic.util.cmd.ExCommand;

import java.util.List;

public class AliasCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(1, true)) {
            if (Database.getUsers().containsUser(args.get(0).getString())) {
                if (sender.hasPermission("alias.other", 48)) {
                    if (args.isLengthEquals(3, false)) {
                        AliasCmd.setAlias(sender, args.get(0).toDbUser(), args.get(1), args.get(2));
                    } else {
                        AliasCmd.setAlias(sender, args.get(0).toDbUser(), args.get(1), null);
                    }
                }
            } else if (args.get(0).equalsIgnoreCase("help")) {
                sender.sendMessageCommandHelp("Set alias for player", "alias [player] " + "<prefix/suffix/nick> [value]");
                sender.sendMessageCommandHelp("Get alias from player", "alias [player] info");
            } else if (sender.isPlayer(true)) {
                if (args.isLengthEquals(2, false)) {
                    AliasCmd.setAlias(sender, sender.getDbUser(), args.get(0), args.get(1));
                } else {
                    AliasCmd.setAlias(sender, sender.getDbUser(), args.get(0), null);
                }
            } else {
                sender.sendMessageCommandHelp("For help", "alias help");
            }
        } else {
            sender.sendMessageCommandHelp("For help", "alias help");
        }

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 1) {
            return Network.getCommandHandler().getPlayerNames();
        }

        if (length == 2) {
            return List.of("info", "prefix", "suffix", "nick");
        }
        return null;
    }

    public static void setAlias(Sender sender, DbUser user, Argument type, Argument name) {
        String msg;
        switch (type.getString()) {

            case "info":
                sender.sendPluginMessage(ChatColor.PERSONAL + "Player: " + ChatColor.VALUE + user.getName() + ChatColor.QUICK_INFO + " (" + user.getUniqueId().toString() + ")");
                sender.sendPluginMessage(ChatColor.PERSONAL + "Prefix: " + ChatColor.VALUE + user.getPrefix());
                sender.sendPluginMessage(ChatColor.PERSONAL + "Suffix: " + ChatColor.VALUE + user.getSuffix());
                sender.sendPluginMessage(ChatColor.PERSONAL + "Nick: " + ChatColor.VALUE + user.getNick());
                break;

            case "prefix":
                if (name == null) {
                    user.setPrefix(null);
                    msg = ChatColor.PERSONAL + "Reset prefix";
                } else {
                    user.setPrefix(name.getString());
                    msg = ChatColor.PERSONAL + "Updated prefix to " + ChatColor.VALUE + name;
                }
                sender.sendPluginMessage(msg + ChatColor.PERSONAL + " from " + ChatColor.VALUE + user.getName() + ChatColor.QUICK_INFO + " (" + user.getUniqueId().toString() + ")");

                Network.getChannel().sendMessage(ChannelUserMessage.getAliasMessage(user.getUniqueId()));
                break;

            case "suffix":
                if (name == null) {
                    user.setSuffix(null);
                    msg = ChatColor.PERSONAL + "Reset suffix";
                } else {
                    user.setSuffix(name.getString());
                    msg = ChatColor.PERSONAL + "Updated suffix to " + ChatColor.VALUE + name;
                }
                sender.sendPluginMessage(msg + ChatColor.PERSONAL + " from " + ChatColor.VALUE + user.getName() + ChatColor.QUICK_INFO + " (" + user.getUniqueId().toString() + ")");

                Network.getChannel().sendMessage(ChannelUserMessage.getAliasMessage(user.getUniqueId()));
                break;

            case "nick":
                if (name == null) {
                    user.setNick(null);
                    msg = ChatColor.PERSONAL + "Reset nick";
                } else {
                    user.setNick(name.getString());
                    msg = ChatColor.PERSONAL + "Updated nick to " + ChatColor.VALUE + name;
                }
                sender.sendPluginMessage(msg + ChatColor.PERSONAL + " from " + ChatColor.VALUE + user.getName() + ChatColor.QUICK_INFO + " (" + user.getUniqueId().toString() + ")");

                Network.getChannel().sendMessage(ChannelUserMessage.getAliasMessage(user.getUniqueId()));
                break;

            default:
                sender.sendMessageCommandHelp("Set alias for player", "alias [player] " + "<prefix/suffix/nick> [value]");
                sender.sendMessageCommandHelp("Get alias from player", "alias [player] info");
                break;
        }
    }

}