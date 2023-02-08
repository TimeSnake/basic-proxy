/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.List;
import net.kyori.adventure.text.Component;

public class CleanupServersCmd implements CommandListener<Sender, Argument> {

    private Code.Permission perm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        if (!sender.hasPermission(this.perm)) {
            return;
        }

        if (!args.isLengthEquals(0, true)) {
            return;
        }

        for (Server server : Network.getServers()) {
            boolean result = Network.deleteServer(server.getName(), false);
            if (result) {
                sender.sendPluginMessage(Component.text("Deleted server ", ExTextColor.PERSONAL)
                        .append(Component.text(server.getName(), ExTextColor.VALUE)));
            } else {
                sender.sendPluginMessage(
                        Component.text("Can not delete server ", ExTextColor.WARNING)
                                .append(Component.text(server.getName(), ExTextColor.VALUE)));
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        return List.of();
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.perm = plugin.createPermssionCode("prx", "network.cleanup_servers");
    }
}
