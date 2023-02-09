/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.List;
import net.kyori.adventure.text.Component;

public class NetworkMsgCmd implements CommandListener<Sender, Argument> {

    private Code perm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        if (sender.hasPermission(this.perm)) {
            if (sender.isPlayer(true)) {
                if (args.isLengthEquals(0, true)) {
                    User user = sender.getUser();
                    user.setListeningNetworkMessages(!user.isListeningNetworkMessages());
                    if (user.isListeningNetworkMessages()) {
                        sender.sendPluginMessage(
                                Component.text("Enabled network messages", ExTextColor.PERSONAL));
                    } else {
                        sender.sendPluginMessage(
                                Component.text("Disabled network messages", ExTextColor.PERSONAL));
                    }
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
            Arguments<Argument> arguments) {
        return null;
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.perm = plugin.createPermssionCode("network.message");
    }
}
