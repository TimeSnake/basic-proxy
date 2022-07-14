package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.ArrayList;
import java.util.List;

public class DeleteTmpServerCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!sender.hasPermission("network.delete_tmp", 50)) {
            return;
        }

        if (!args.isLengthEquals(1, true)) {
            return;
        }

        String serverName = args.getString(0);

        boolean result = Network.deleteServer(serverName);

        if (result) {
            sender.sendPluginMessage(ChatColor.PERSONAL + "Deleted server " + ChatColor.VALUE + serverName);
        } else {
            sender.sendPluginMessage(ChatColor.WARNING + "Can not delete server " + ChatColor.VALUE + serverName);
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return args.length() == 1 ? Network.getCommandHandler().getServerNames() : new ArrayList<>(0);
    }
}
