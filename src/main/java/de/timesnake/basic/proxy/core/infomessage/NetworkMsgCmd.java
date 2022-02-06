package de.timesnake.basic.proxy.core.infomessage;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.basic.util.cmd.Arguments;
import de.timesnake.library.basic.util.cmd.CommandListener;
import de.timesnake.library.basic.util.cmd.ExCommand;

import java.util.List;

public class NetworkMsgCmd implements CommandListener<Sender, Argument> {

    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        handleNetworkMessageCmd(sender, args);

    }

    public static void handleNetworkMessageCmd(Sender sender, Arguments<Argument> args) {
        if (sender.hasPermission("network.message", 9)) {
            if (sender.isPlayer(true)) {
                if (args.isLengthEquals(0, true)) {
                    User user = sender.getUser();
                    user.setListeningNetworkMessages(!user.isListeningNetworkMessages());
                    if (user.isListeningNetworkMessages()) {
                        sender.sendPluginMessage(ChatColor.PERSONAL + "Enabled network messages");
                    } else {
                        sender.sendPluginMessage(ChatColor.PERSONAL + "Disabled network messages");
                    }
                }
            }

        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> arguments) {
        return null;
    }
}