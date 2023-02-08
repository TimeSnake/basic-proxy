/*
 * Copyright (C) 2023 timesnake
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
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        switch (cmd.getName().toLowerCase()) {
            case "netmute", "mute" -> {
                if (!sender.hasPermission(this.mutePerm)) {
                    return;
                }

                if (!args.isLengthHigherEquals(2, true)) {
                    sender.sendMessageCommandHelp("Mute a player", "netmute <player> <reason>");
                    return;
                }
                if (!args.get(0).isPlayerDatabaseName(true)) {
                    return;
                }

                Network.getPunishmentManager()
                        .mutePlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
            }
            case "netkick", "kick" -> {
                if (!sender.hasPermission(this.kickPerm)) {
                    return;
                }

                if (!args.isLengthHigherEquals(2, true)) {
                    sender.sendMessageCommandHelp("Kick a player", "netkick <player> <reason>");
                    return;
                }

                if (!args.get(0).isPlayerName(true)) {
                    return;
                }

                Network.getPunishmentManager()
                        .kickPlayer(sender, args.get(0).toUser(), args.toMessage(1));
            }
            case "netunmute", "unmute" -> {
                if (!sender.hasPermission(this.unmutePerm)) {
                    return;
                }

                if (!args.isLengthHigherEquals(1, true)) {
                    sender.sendMessageCommandHelp("Unmute a player", "unmute <player>");
                    return;
                }

                if (!args.get(0).isPlayerDatabaseName(true)) {
                    return;
                }

                Network.getPunishmentManager().unmutePlayer(sender, args.get(0).toDbUser());
            }
            case "nettempban", "nettmpban", "tempban", "tmpban" -> {
                if (!sender.hasPermission(this.tempbanPerm)) {
                    return;
                }

                if (!args.isLengthHigherEquals(3, true)) {
                    sender.sendMessageCommandHelp("Temp-ban a player", "nettempban <player> " +
                            "<duration> <reason> \n" + ChatColor.QUICK_INFO + "duration:" +
                            " 1year;1month;1day;1hour;1min;1sec");
                    return;
                }

                if (!args.get(0).isPlayerDatabaseName(true)) {
                    return;
                }

                Network.getPunishmentManager().tempBanPlayer(sender, args.get(0).toDbUser(),
                        args.get(1).getString(), args.toMessage(2));
            }
            case "netban", "ban" -> {
                if (!sender.hasPermission(this.banPerm)) {
                    return;
                }

                if (!args.isLengthHigherEquals(2, true)) {
                    sender.sendMessageCommandHelp("Ban a player", "netban <player> <reason>");
                    return;
                }

                if (!args.get(0).isPlayerDatabaseName(true)) {
                    return;
                }

                Network.getPunishmentManager()
                        .banPlayer(sender, args.get(0).toDbUser(), args.toMessage(1));
            }
            case "netunban", "unban", "pardon", "netpardon" -> {
                if (!sender.hasPermission(this.unbanPerm)) {
                    return;
                }

                if (!args.isLengthEquals(1, true)) {
                    sender.sendMessageCommandHelp("Unban a player", "unban <player>");
                    return;
                }

                if (!args.get(0).isPlayerDatabaseName(true)) {
                    return;
                }

                Network.getPunishmentManager()
                        .unbanPlayer(sender, args.get(0).toDbUser().getUniqueId());
            }
        }

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
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
