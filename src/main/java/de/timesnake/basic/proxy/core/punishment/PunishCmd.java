/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.punishment;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.List;

public class PunishCmd implements CommandListener<Sender, Argument> {

    private Code.Permission mutePerm;
    private Code.Permission kickPerm;
    private Code.Permission unmutePerm;
    private Code.Permission tempbanPerm;
    private Code.Permission banPerm;
    private Code.Permission unbanPerm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        switch (cmd.getName().toLowerCase()) {
            case "netmute":
            case "mute":
                if (sender.hasPermission(this.mutePerm)) {
                    if (args.isLengthHigherEquals(2, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Network.getPunishmentManager().mutePlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
                            return;
                        }
                    } else sender.sendMessageCommandHelp("Mute a player", "netmute <player> <reason>");
                }
                break;

            case "netkick":
            case "kick":
                if (sender.hasPermission(this.kickPerm)) {
                    if (args.isLengthHigherEquals(2, true)) {
                        if (args.get(0).isPlayerName(true)) {
                            Network.getPunishmentManager().kickPlayer(sender, args.get(0).toUser(), args.toMessage(1));
                        }
                    } else sender.sendMessageCommandHelp("Kick a player", "netkick <player> <reason>");
                }
                break;

            case "netunmute":
            case "unmute":
                if (sender.hasPermission(this.unmutePerm)) {
                    if (args.isLengthHigherEquals(1, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Network.getPunishmentManager().unmutePlayer(sender, args.get(0).toDbUser());
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
                if (sender.hasPermission(this.tempbanPerm)) {
                    if (args.isLengthHigherEquals(3, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Network.getPunishmentManager().tempBanPlayer(sender, args.get(0).toDbUser(), args.get(1).getString(),
                                    args.toMessage(2));
                        }
                    } else {
                        sender.sendMessageCommandHelp("Temp-ban a player", "nettempban <player> " +
                                "<duration> <reason> \n" + ChatColor.QUICK_INFO + "duration:" +
                                " 1year;1month;1day;1hour;1min;1sec");
                    }
                }
                break;

            case "netban":
            case "ban":
                if (sender.hasPermission(this.banPerm)) {
                    if (args.isLengthHigherEquals(2, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Network.getPunishmentManager().banPlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
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
                if (sender.hasPermission(this.unbanPerm)) {
                    if (args.isLengthEquals(1, true)) {
                        if (args.get(0).isPlayerDatabaseName(true)) {
                            Network.getPunishmentManager().unbanPlayer(sender, args.get(0).toDbUser().getUniqueId());
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

        if (length == 2 && (args.getString(0).equalsIgnoreCase("tempban")
                || args.getString(0).equalsIgnoreCase("tmpban")
                || args.getString(0).equalsIgnoreCase("nettempban")
                || args.getString(0).equalsIgnoreCase("nettmpban"))) {
            return List.of("1year;1month;1day;1hour;1min;1sec");
        }
        return null;
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.mutePerm = plugin.createPermssionCode("pun", "punish.netmute");
        this.unmutePerm = plugin.createPermssionCode("pun", "punish.netunmute");
        this.kickPerm = plugin.createPermssionCode("pun", "punish.kick");
        this.tempbanPerm = plugin.createPermssionCode("pun", "punish.tempban");
        this.banPerm = plugin.createPermssionCode("pun", "punish.ban");
        this.unbanPerm = plugin.createPermssionCode("pun", "punish.unban");
    }
}
