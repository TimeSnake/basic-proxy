package de.timesnake.basic.proxy.core.infomessage;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

public class NetworkMsgCmd implements CommandListener<Sender, Argument> {

    public static void handleNetworkMessageCmd(Sender sender, Arguments<Argument> args) {
        if (sender.hasPermission("network.message", 9)) {
            if (sender.isPlayer(true)) {
                if (args.isLengthEquals(0, true)) {
                    User user = sender.getUser();
                    user.setListeningNetworkMessages(!user.isListeningNetworkMessages());
                    if (user.isListeningNetworkMessages()) {
                        sender.sendPluginMessage(Component.text("Enabled network messages", ExTextColor.PERSONAL));
                    } else {
                        sender.sendPluginMessage(Component.text("Disabled network messages", ExTextColor.PERSONAL));
                    }
                }
            }

        }
    }

    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        handleNetworkMessageCmd(sender, args);

    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> arguments) {
        return null;
    }
}
