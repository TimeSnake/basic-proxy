package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;

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

        boolean result = Network.deleteServer(serverName, false);

        if (result) {
            sender.sendPluginMessage(Component.text("Deleted server ", ExTextColor.PERSONAL)
                    .append(Component.text(serverName, ExTextColor.VALUE)));
        } else {
            sender.sendPluginMessage(Component.text("Can not delete server ", ExTextColor.WARNING)
                    .append(Component.text(serverName, ExTextColor.VALUE)));
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return args.length() == 1 ? Network.getCommandHandler().getServerNames() : new ArrayList<>(0);
    }
}
