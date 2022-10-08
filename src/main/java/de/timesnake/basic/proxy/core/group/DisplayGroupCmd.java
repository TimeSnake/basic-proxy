/*
 * basic-proxy.main
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

package de.timesnake.basic.proxy.core.group;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DisplayGroupCmd implements CommandListener<Sender, Argument> {

    private Code.Permission perm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!sender.hasPermission(this.perm)) {
            return;
        }

        if (!args.isLengthHigherEquals(2, true)) {
            return;
        }

        if (!args.get(0).isPlayerDatabaseName(true)) {
            return;
        }

        if (!args.isLengthEquals(3, true)) {
            return;
        }

        String groupName = args.getString(2).toLowerCase();
        DisplayGroup group = Network.getDisplayGroup(groupName);

        if (group == null) {
            sender.sendPluginMessage(Component.text("Display group ", ExTextColor.WARNING)
                    .append(Component.text(groupName, ExTextColor.VALUE))
                    .append(Component.text(" not exists", ExTextColor.WARNING)));
        }

        DbUser user = args.get(0).toDbUser();
        String userName = user.getName();
        UUID uuid = user.getUniqueId();
        String action = args.getString(1).toLowerCase();

        switch (action) {
            case "add" -> {
                if (user.getDisplayGroupNames().contains(groupName)) {
                    sender.sendPluginMessage(Component.text(userName).color(ExTextColor.VALUE)
                            .append(Component.text(" is already member of display group ").color(ExTextColor.WARNING))
                            .append(Component.text(groupName).color(ExTextColor.VALUE)));
                    return;
                }

                user.addDisplayGroup(groupName);
                sender.sendPluginMessage(Component.text("Added ").color(ExTextColor.PERSONAL)
                        .append(Component.text(userName).color(ExTextColor.VALUE))
                        .append(Component.text(" to display group ").color(ExTextColor.PERSONAL))
                        .append(Component.text(groupName).color(ExTextColor.VALUE)));
            }
            case "remove" -> {
                if (!user.getDisplayGroupNames().contains(groupName)) {
                    sender.sendPluginMessage(Component.text(user.getName()).color(ExTextColor.VALUE)
                            .append(Component.text(" is not a member of display group ").color(ExTextColor.WARNING))
                            .append(Component.text(groupName).color(ExTextColor.VALUE)));
                    return;
                }

                user.removeDisplayGroup(groupName);
                sender.sendPluginMessage(Component.text("Removed ").color(ExTextColor.PERSONAL)
                        .append(Component.text(userName).color(ExTextColor.VALUE))
                        .append(Component.text(" from display group ").color(ExTextColor.PERSONAL))
                        .append(Component.text(groupName).color(ExTextColor.VALUE)));
            }
            default -> {
                return;
            }
        }

        if (Network.isUserOnline(uuid)) {
            Network.getUser(uuid).updateDisplayGroup();
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.length() == 1) {
            return Network.getCommandHandler().getPlayerNames();
        } else if (args.length() == 2) {
            return List.of("add", "remove");
        } else if (args.length() == 3) {
            return Network.getCommandHandler().getDisplayGroupNames();
        }
        return new ArrayList<>(0);
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.perm = plugin.createPermssionCode("prx", "chat.display_group");
    }
}
