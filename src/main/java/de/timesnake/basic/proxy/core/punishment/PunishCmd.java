package de.timesnake.basic.proxy.core.punishment;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class PunishCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        switch (cmd.getName().toLowerCase()) {
            case "netmute":
            case "mute":
                if (sender.hasPermission("punish.netmute", 15)) {
                    if (args.isLengthHigherEquals(2, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Punishments.mutePlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
                            return;
                        }
                    } else sender.sendMessageCommandHelp("Mute a player", "netmute <player> <reason>");
                }
                break;

            case "netkick":
            case "kick":
                if (sender.hasPermission("punish.netkick", 14)) {
                    if (args.isLengthHigherEquals(2, true)) {
                        if (args.get(0).isPlayerName(true)) {
                            Punishments.kickPlayer(sender, (User) args.get(0).toUser(), args.toMessage(1));
                        }
                    } else sender.sendMessageCommandHelp("Kick a player", "netkick <player> <reason>");
                }
                break;

            case "netunmute":
            case "unmute":
                if (sender.hasPermission("punish.netunmute", 18)) {
                    if (args.isLengthHigherEquals(1, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Punishments.unmutePlayer(sender, args.get(0).toDbUser());
                            return;
                        }
                    } else {
                        sender.sendMessageCommandHelp("Unmute a player", "unmute <player>");
                    }
                }
                break;

            case "nettempban":
            case "nettmpban":
            case "tempban":
            case "tmpban":
                if (sender.hasPermission("punish.nettempban", 16)) {
                    if (args.isLengthHigherEquals(3, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Punishments.tempBanPlayer(sender, args.get(0).toDbUser(), args.get(1).getString(), args.toMessage(2));
                        }
                    } else {
                        sender.sendMessageCommandHelp("Temp-ban a player", "nettempban <player> " + "<duration> <reason> \n" + ChatColor.QUICK_INFO + "duration:" + " 1year;1month;1day;1hour;1min;1sec");
                    }
                }
                break;

            case "netban":
            case "ban":
                if (sender.hasPermission("punish.netban", 13)) {
                    if (args.isLengthHigherEquals(2, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Punishments.banPlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
                        }
                    } else {
                        sender.sendMessageCommandHelp("Ban a player", "netban <player> <reason>");
                    }
                }
                break;

            case "netunban":
            case "unban":
            case "pardon":
            case "netpardon":
                if (sender.hasPermission("punish.netunban", 17)) {
                    if (args.isLengthEquals(1, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Punishments.unbanPlayer(sender, args.get(0).toDbUser().getUniqueId());
                            return;
                        } else {
                            sender.sendMessageCommandHelp("Unban a player", "unban <player>");
                        }
                    }
                }
                break;
        }

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 1) {
            return Network.getCommandHandler().getPlayerNames();
        }

        if (length == 2 && (args.getString(0).equalsIgnoreCase("tempban") || args.getString(0).equalsIgnoreCase("tmpban") || args.getString(0).equalsIgnoreCase("nettempban") || args.getString(0).equalsIgnoreCase("nettmpban"))) {
            return List.of("1year;1month;1day;1hour;1min;1sec");
        }
        return null;
    }
}
