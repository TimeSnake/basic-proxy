package de.timesnake.basic.proxy.core.rule;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class RuleCmd implements CommandListener<Sender, Argument> {


    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.isLengthEquals(0, false)) {
            sender.sendMessage("");
            sender.sendPluginMessage(ChatColor.PERSONAL + "" + ChatColor.BOLD + "Version: " + ChatColor.VALUE + Network.getRuleManager().getVersion());
            sender.sendPluginMessage(Chat.getLongLineSeparator());
            for (RuleSection section : Network.getRuleManager().getSections()) {
                sender.sendPluginMessage(ChatColor.PERSONAL + "" + section.getNumber() + " " + ChatColor.VALUE + section.getName());
            }
            sender.sendPluginMessage(Chat.getLongLineSeparator());
            sender.sendMessageCommandHelp("Open section", "rule <number>");
            return;
        }

        if (!args.isLengthEquals(1, true) || !args.get(0).isInt(true)) {
            sender.sendMessageCommandHelp("Open rule menu", "rule");
            sender.sendMessageCommandHelp("Open section", "rule <number>");
            return;
        }

        Integer number = args.get(0).toInt();

        RuleSection section = Network.getRuleManager().getSection(number);

        if (section == null) {
            sender.sendMessageNotExist(String.valueOf(number), 128, "rule");
            return;
        }

        sender.sendMessage("");
        sender.sendPluginMessage(Chat.getLongLineSeparator());
        sender.sendPluginMessage(ChatColor.PERSONAL + "" + ChatColor.BOLD + "" + section.getNumber() + " " + ChatColor.VALUE + section.getName());
        sender.sendPluginMessage(Chat.getLongLineSeparator());
        for (RuleParagraph paragraph : section.getParagraphs()) {
            for (String part : paragraph.getParts()) {
                sender.sendPluginMessage(ChatColor.PERSONAL + paragraph.getName() + ChatColor.VALUE + " " + part);
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return null;
    }


}
