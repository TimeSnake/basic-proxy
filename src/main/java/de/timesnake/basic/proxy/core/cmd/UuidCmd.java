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

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.UUID;

public class UuidCmd implements CommandListener<Sender, Argument> {

    private Code.Permission permCode;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!sender.hasPermission(this.permCode)) {
            return;
        }

        if (!args.isLengthEquals(1, true)) {
            return;
        }

        Argument arg = args.get(0);

        if (arg.isUUID(false)) {
            UUID uuid = arg.toUUIDOrExit(true);
            DbUser dbUser = Database.getUsers().getUser(uuid);
            if (dbUser == null) {
                sender.sendPluginMessage(Component.text("Unknown user", ExTextColor.WARNING));
                return;
            }

            String name = dbUser.getName();

            if (name == null) {
                sender.sendPluginMessage(Component.text("Unknown user", ExTextColor.WARNING));
                return;
            }

            sender.sendPluginMessage(Component.text(name, ExTextColor.PERSONAL, TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.copyToClipboard(name))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy"))));

        } else {
            String name = arg.getString();

            DbUser dbUser = Database.getUsers().getUser(name);
            if (dbUser == null) {
                sender.sendPluginMessage(Component.text("Unknown user", ExTextColor.WARNING));
                return;
            }

            UUID uuid = dbUser.getUniqueId();

            sender.sendPluginMessage(Component.text(uuid.toString(), ExTextColor.PERSONAL, TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.copyToClipboard(uuid.toString()))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy"))));
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return CommandListener.super.getTabCompletion(cmd, args);
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.permCode = plugin.createPermssionCode("id", "network.uuid");
    }
}
