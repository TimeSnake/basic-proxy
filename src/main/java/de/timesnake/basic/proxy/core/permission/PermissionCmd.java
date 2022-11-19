/*
 * workspace.basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.basic.proxy.core.permission;

import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

public class PermissionCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(1, true)) {
            if (args.get(0).equalsIgnoreCase("user")) {
                this.handleUserPermissionCmd(sender, args);
            } else if (args.get(0).equalsIgnoreCase("group")) {
                this.handleGroupPermissionCmd(sender, args);
            } else if (args.get(0).equalsIgnoreCase("reload")) {
                sender.sendPluginMessage(Component.text("Permissions reloaded", ExTextColor.PERSONAL));
                for (User user : Network.getUsers()) {
                    user.updatePermissions(true);
                }
            } else if (args.get(0).equalsIgnoreCase("help")) {
                sender.sendMessageCommandHelp("Set user permission", "perm user <user> add/remove> " + "<permission>");
                sender.sendMessageCommandHelp("Set user permgroup", "perm user <user> " + "setgroup/removegroup " +
                        "<group>");
                sender.sendMessageCommandHelp("Create permgroup", "perm group <group> create <rank>");
                sender.sendMessageCommandHelp("Delete permgroup", "perm group <group> delete");
                sender.sendMessageCommandHelp("Add/Remove permission from group", "perm group <group> " + "add/remove" +
                        " <permission> <mode>");
                sender.sendMessageCommandHelp("Set/Remove inheritance", "perm group <group> " + "setinherit" +
                        "/removeinherit <group>");
                sender.sendMessageCommandHelp("Reload permissions", "perm reload");
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
                    case "add":
                        if (args.isLengthHigherEquals(5, true) && args.get(4).isPermissionStatus(true)) {
                            Network.getPermissionHandler().addPlayerPermission(sender, user,
                                    args.get(3).toLowerCase(), args.get(4).toPermissionStatus());
                        }
                        break;
                    case "remove":
                        Network.getPermissionHandler().removePlayerPermission(sender, user, args.get(3).toLowerCase());
                        break;
                    case "setgroup":
                        Network.getPermissionHandler().setPlayerGroup(sender, user, args.get(3).toLowerCase());
                        break;
                    case "removegroup":
                        Network.getPermissionHandler().setPlayerGroup(sender, user, null);
                        break;
                    default:

                }
            }
        }
    }

    private void handleGroupPermissionCmd(Sender sender, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(4, true)) {
            String groupName = args.getString(1).toLowerCase();
            PermGroup group = Network.getPermGroup(groupName);
            switch (args.getString(2).toLowerCase()) {
                case "create":
                    if (args.isLengthEquals(4, true) && args.get(3).isInt(true)) {
                        Network.getPermissionHandler().createGroup(sender, groupName, args.get(3).toInt());
                    } else {
                        sender.sendMessageCommandHelp("Create permgroup", "perm group <group> create" + " <rank> ");
                    }
                    break;
                case "delete":
                    Network.getPermissionHandler().deleteGroup(sender, groupName);
                    break;
                case "add":
                    if (args.isLengthHigherEquals(5, true) && args.get(4).isPermissionStatus(true)) {
                        Network.getPermissionHandler().addGroupPermission(sender, groupName,
                                args.get(3).toLowerCase(), args.get(4).toPermissionStatus());
                    }
                    break;
                case "remove":
                    Network.getPermissionHandler().removeGroupPermission(sender, groupName, args.get(3).toLowerCase());
                    break;
                case "setinheritance":
                case "setinherit":
                    Network.getPermissionHandler().setGroupInheritance(sender, groupName, args.get(3).toLowerCase());
                    break;
                case "removeinheritance":
                case "remowinherit":
                    Network.getPermissionHandler().removeGroupInheritance(sender, groupName);
                    break;
                default:

            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 0) {
            return null;
        }
        if (length == 1) {
            return List.of("user", "group");
        }
        if (args.getString(0).equalsIgnoreCase("user")) {
            if (length == 2) {
                return Network.getCommandHandler().getPlayerNames();
            }
            if (length == 3) {
                return List.of("add", "remove", "setgroup", "removegroup");
            }
            if (length == 4) {
                if (args.getString(2).equalsIgnoreCase("setgroup") || args.getString(2).equalsIgnoreCase("removegroup"
                )) {
                    return Network.getCommandHandler().getPermGroupNames();
                }
            }
            return null;
        }

        if (args.getString(0).equalsIgnoreCase("group")) {
            if (length == 2) {
                return Network.getCommandHandler().getPermGroupNames();
            }
            if (length == 3) {
                return List.of("add", "remove", "create", "delete", "setinherit", "removeinherit");
            }

            if (length == 4) {
                if (args.getString(3).equalsIgnoreCase("setinherit") || args.getString(3).equalsIgnoreCase(
                        "removeinherit")) {
                    return Network.getCommandHandler().getPermGroupNames();
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
