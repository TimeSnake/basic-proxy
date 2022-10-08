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

package de.timesnake.basic.proxy.core.rule;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;

public class RuleCmd implements CommandListener<Sender, Argument> {


    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        Component text = Chat.getSenderPlugin(Plugin.NETWORK)
                .append(Component.text("https://timesnake.de/rules/", ExTextColor.PERSONAL))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://timesnake.de/rules/"))
                .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to open")));

        sender.sendMessage(text);

        /*
        if (args.isLengthEquals(0, false)) {
            sender.sendMessage("");
            sender.sendPluginMessage(ChatColor.PERSONAL + "" + ChatColor.BOLD + "Version: " +
                    ChatColor.VALUE + Network.getRuleManager().getVersion());
            sender.sendPluginMessage(Chat.getLongLineSeparator());
            for (RuleSection section : Network.getRuleManager().getSections()) {
                sender.sendPluginMessage(ChatColor.PERSONAL + "" + section.getName() + " " + ChatColor.VALUE +
                section.getTitle());
            }
            sender.sendPluginMessage(Chat.getLongLineSeparator());
            sender.sendMessageCommandHelp("Open section", "rule <name>");
            return;
        }

        if (!args.isLengthEquals(1, true)) {
            sender.sendMessageCommandHelp("Open rule menu", "rule");
            sender.sendMessageCommandHelp("Open section", "rule <name>");
            return;
        }

        String sectionName = args.getString(0);

        RuleSection section = Network.getRuleManager().getSection(sectionName);

        if (section == null) {
            sender.sendMessageNotExist(String.valueOf(sectionName), 128, "rule");
            return;
        }

        sender.sendMessage("");
        sender.sendPluginMessage(Chat.getLongLineSeparator());
        sender.sendPluginMessage(ChatColor.PERSONAL + "" + ChatColor.BOLD + "" + section.getName() + " " +
                ChatColor.VALUE + section.getTitle());
        sender.sendPluginMessage(Chat.getLongLineSeparator());
        for (RuleParagraph paragraph : section.getParagraphs()) {
            for (String part : paragraph.getParts()) {
                sender.sendPluginMessage(ChatColor.PERSONAL + paragraph.getName() + ChatColor.VALUE + " " + part);
            }
        }

         */
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return List.of();
    }

    @Override
    public void loadCodes(de.timesnake.library.extension.util.chat.Plugin plugin) {

    }


}
