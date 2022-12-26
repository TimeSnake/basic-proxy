/*
 * Copyright (C) 2022 timesnake
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;

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
            return;
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
