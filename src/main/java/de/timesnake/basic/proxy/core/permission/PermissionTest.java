package de.timesnake.basic.proxy.core.permission;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class PermissionTest implements CommandListener<Sender, Argument> {


    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!sender.isPlayer(true)) {
            return;
        }

        if (!args.isLengthHigherEquals(1, true)) {
            return;
        }

        if (sender.hasPermission(args.getString(0))) {
            sender.sendPluginMessage(ChatColor.PERSONAL + "You have the permission " + args.getString(0));
        } else {
            sender.sendPluginMessage(ChatColor.PERSONAL + "You have not the permission " + args.getString(0));
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return null;
    }
}
