/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

public class UuidCmd implements CommandListener<Sender, Argument> {

    private Code permCode;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
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

            sender.sendPluginMessage(
                    Component.text(name, ExTextColor.PERSONAL, TextDecoration.UNDERLINED)
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

            sender.sendPluginMessage(
                    Component.text(uuid.toString(), ExTextColor.PERSONAL, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.copyToClipboard(uuid.toString()))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to copy"))));
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        return List.of();
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.permCode = plugin.createPermssionCode("network.uuid");
    }
}
