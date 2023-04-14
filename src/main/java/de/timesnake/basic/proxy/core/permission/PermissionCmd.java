/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.permission;

import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.List;
import net.kyori.adventure.text.Component;

public class PermissionCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        if (args.isLengthHigherEquals(1, true)) {
            if (args.get(0).equalsIgnoreCase("user")) {
                this.handleUserPermissionCmd(sender, args);
            } else if (args.get(0).equalsIgnoreCase("group")) {
                this.handleGroupPermissionCmd(sender, args);
            } else if (args.get(0).equalsIgnoreCase("reload")) {
                sender.sendPluginMessage(
                        Component.text("Permissions reloaded", ExTextColor.PERSONAL));
                for (User user : Network.getUsers()) {
                    user.updatePermissions(true);
                }
            } else if (args.get(0).equalsIgnoreCase("help")) {
                sender.sendTDMessageCommandHelp("Set user permission",
                        "perm user <user> add/remove> " + "<permission>");
                sender.sendTDMessageCommandHelp("Set user permgroup",
                        "perm user <user> " + "setgroup/removegroup " +
                                "<group>");
                sender.sendTDMessageCommandHelp("Create permgroup",
                        "perm group <group> create <rank>");
                sender.sendTDMessageCommandHelp("Delete permgroup", "perm group <group> delete");
                sender.sendTDMessageCommandHelp("Add/Remove permission from group",
                        "perm group <group> " + "add/remove" +
                                " <permission> <mode>");
                sender.sendTDMessageCommandHelp("Set/Remove inheritance",
                        "perm group <group> " + "setinherit" +
                                "/removeinherit <group>");
                sender.sendTDMessageCommandHelp("Reload permissions", "perm reload");
            } else {
                sender.sendMessageUseHelp("perm help");
            }
        } else {
            sender.sendMessageUseHelp("perm help");
        }

    }

    private void handleUserPermissionCmd(Sender sender, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(2, true) && args.get(1).isPlayerDatabaseName(true)) {

            DbUser user = Database.getUsers().getUser(args.getString(1));

            if (args.isLengthHigherEquals(4, true)) {
                switch (args.getString(2).toLowerCase()) {
                    case "add" -> {
                        if (args.isLengthHigherEquals(5, true) && args.get(4)
                                .isPermissionStatus(true)) {
                            Network.getPermissionHandler().addPlayerPermission(sender, user,
                                    args.get(3).toLowerCase(), args.get(4).toPermissionStatus());
                        }
                    }
                    case "remove" -> Network.getPermissionHandler()
                            .removePlayerPermission(sender, user, args.get(3).toLowerCase());
                    case "setgroup" -> Network.getPermissionHandler()
                            .setPlayerGroup(sender, user, args.get(3).toLowerCase());
                    case "removegroup" ->
                            Network.getPermissionHandler().setPlayerGroup(sender, user, null);
                    default -> {
                    }
                }
            }
        }
    }

    private void handleGroupPermissionCmd(Sender sender, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(4, true)) {
            String groupName = args.getString(1).toLowerCase();
            PermGroup group = Network.getGroupManager().getPermGroup(groupName);
            switch (args.getString(2).toLowerCase()) {
                case "create" -> {
                    if (args.isLengthEquals(4, true) && args.get(3).isInt(true)) {
                        Network.getPermissionHandler()
                                .createGroup(sender, groupName, args.get(3).toInt());
                    } else {
                        sender.sendTDMessageCommandHelp("Create permgroup",
                                "perm group <group> create" + " <rank> ");
                    }
                }
                case "delete" -> Network.getPermissionHandler().deleteGroup(sender, groupName);
                case "add" -> {
                    if (args.isLengthHigherEquals(5, true) && args.get(4)
                            .isPermissionStatus(true)) {
                        Network.getPermissionHandler().addGroupPermission(sender, groupName,
                                args.get(3).toLowerCase(), args.get(4).toPermissionStatus());
                    }
                }
                case "remove" -> Network.getPermissionHandler()
                        .removeGroupPermission(sender, groupName, args.get(3).toLowerCase());
                case "setinheritance", "setinherit" -> Network.getPermissionHandler()
                        .setGroupInheritance(sender, groupName, args.get(3).toLowerCase());
                case "removeinheritance", "remowinherit" ->
                        Network.getPermissionHandler().removeGroupInheritance(sender, groupName);
                default -> {
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 0) {
            return null;
        }
        if (length == 1) {
            return List.of("user", "group");
        }
        if (args.getString(0).equalsIgnoreCase("user")) {
            if (length == 2) {
                return Network.getCommandManager().getPlayerNames();
            }
            if (length == 3) {
                return List.of("add", "remove", "setgroup", "removegroup");
            }
            if (length == 4) {
                if (args.getString(2).equalsIgnoreCase("setgroup") || args.getString(2)
                        .equalsIgnoreCase("removegroup"
                        )) {
                    return Network.getCommandManager().getPermGroupNames();
                }
            }
            return null;
        }

        if (args.getString(0).equalsIgnoreCase("group")) {
            if (length == 2) {
                return Network.getCommandManager().getPermGroupNames();
            }
            if (length == 3) {
                return List.of("add", "remove", "create", "delete", "setinherit", "removeinherit");
            }

            if (length == 4) {
                if (args.getString(3).equalsIgnoreCase("setinherit") || args.getString(3)
                        .equalsIgnoreCase(
                                "removeinherit")) {
                    return Network.getCommandManager().getPermGroupNames();
                }
            }
            return null;
        }
        return null;
    }

    @Override
    public void loadCodes(Plugin plugin) {

    }

}
