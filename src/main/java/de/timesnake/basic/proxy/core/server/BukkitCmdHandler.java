package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.database.util.object.Status;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class BukkitCmdHandler implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!sender.hasPermission("network.server.cmd", 32)) {
            return;
        }

        if (!args.isLengthHigherEquals(1, true)) {
            return;
        }

        if (args.get(0).equalsIgnoreCase("all")) {
            if (!args.isLengthHigherEquals(2, true)) {
                return;
            }

            if (args.get(1).equalsIgnoreCase("start")) {
                for (Server server : Network.getServers()) {
                    if (server.getStatus().equals(Status.Server.OFFLINE)) {
                        server.start();
                    }
                }
            } else if (args.get(1).equalsIgnoreCase("stop")) {
                this.stopAllServers();
                sender.sendPluginMessage(ChatColor.PERSONAL + "All servers stopped");
            } else {
                sender.sendPluginMessage(ChatColor.WARNING + "Only commands " + ChatColor.VALUE + "start " + ChatColor.WARNING + "and " + ChatColor.VALUE + "stop " + ChatColor.WARNING + " are allowed on " + "the all servers command");
            }

        } else if (args.get(0).isServerName(true)) {
            if (!args.isLengthHigherEquals(2, true)) {
                sender.sendMessageCommandHelp("Execute command on server", "cmd <server> <cmd> [args]");
                return;
            }

            if (args.get(2).equalsIgnoreCase("start")) {
                this.startServer(sender, args);
                return;
            }

            Server server = args.get(0).toServer();
            String bukkitCmd = args.toMessage(1);
            server.execute(bukkitCmd);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Executed command on server " + ChatColor.VALUE + server.getName() + ChatColor.PERSONAL + ": " + bukkitCmd);
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> arguments) {
        return null;
    }

    public void startServer(Sender sender, Arguments<Argument> args) {
        if (Network.getServer(args.get(1).toLowerCase()) == null) {
            sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + args.get(1).toLowerCase() + ChatColor.WARNING + " doesn't exist!");
            return;
        }

        Server server = Network.getServer(args.get(1).toLowerCase());

        if (server.getStatus().equals(Status.Server.OFFLINE)) {
            sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + args.get(1).toLowerCase() + ChatColor.WARNING + " is already online!");
            return;
        }

        if (args.isLengthEquals(3, false) && args.get(3).isInt(true)) {
            server.setMaxPlayers(args.get(3).toInt());
        }

        this.handleServerCmd(sender, server);
    }

    public void handleServiceCommand(Sender sender, Arguments<Argument> args) {
        if (args.isLengthEquals(1, true)) {
            Server server = Network.getServer(args.get(0).getString());
            handleService(sender, args, server);
        } else {
            if (sender.isConsole(false)) {
                sender.sendMessageTooFewArguments();
                return;
            }

            Server server = sender.getUser().getServer();
            handleService(sender, args, server);
        }
    }

    private void handleService(Sender sender, Arguments<Argument> args, Server server) {
        if (server == null) {
            sender.sendMessageServerNameNotExist(args.get(0).getString());
            return;
        }

        if (server.getStatus().equals(Status.Server.SERVICE)) {
            sender.sendPluginMessage(ChatColor.PERSONAL + "Changed status of server " + ChatColor.VALUE + server.getName() + ChatColor.PERSONAL + " to service");
        } else {
            sender.sendPluginMessage(ChatColor.PERSONAL + "Changed status of server " + ChatColor.VALUE + server.getName() + ChatColor.PERSONAL + " to online");
        }
    }

    public void handleServerCmd(Sender sender, Server server) {
        boolean isStart = server.start();
        if (!isStart) {
            sender.sendMessage(Chat.getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING + "Error while starting server " + ChatColor.VALUE + server.getName());
            return;
        }

        sender.sendMessage(Chat.getSenderPlugin(Plugin.NETWORK) + ChatColor.PERSONAL + "Started server " + ChatColor.VALUE + server.getName());
        if (!sender.isConsole(false)) {
            Network.printText(Plugin.NETWORK, "Started server " + server.getName());
        }
    }

    public void stopAllServers() {
        for (Server server : Network.getServers()) {
            if (server.getStatus() != null && !server.getStatus().equals(Status.Server.OFFLINE)) {
                server.stop();
            }
        }
    }
}
