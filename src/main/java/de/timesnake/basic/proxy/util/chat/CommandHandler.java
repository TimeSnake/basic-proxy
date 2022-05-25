package de.timesnake.basic.proxy.util.chat;

import de.timesnake.basic.proxy.core.group.Group;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.library.basic.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CommandHandler {

    private final HashMap<String, ExCommand<Sender, Argument>> commands = new HashMap<>();

    public void addCommand(net.md_5.bungee.api.plugin.Plugin mainClass, PluginManager pm, String cmd,
                           CommandListener<Sender, Argument> listener, Plugin basicPlugin) {
        this.commands.put(cmd, new ExCommand<>(cmd, listener, basicPlugin));
        pm.registerCommand(mainClass, new Command(cmd));
    }

    public void addCommand(net.md_5.bungee.api.plugin.Plugin mainClass, PluginManager pm, String cmd,
                           List<String> aliases, CommandListener<Sender, Argument> listener, Plugin basicPlugin) {
        this.commands.put(cmd, new ExCommand<>(cmd, listener, basicPlugin));
        String[] aliasArray = new String[aliases.size()];
        pm.registerCommand(mainClass, new Command(cmd, aliases.toArray(aliasArray)));
    }

    public List<String> getPlayerNames() {
        List<String> names = new ArrayList<>();
        for (User user : Network.getUsers()) {
            if (user.isAirMode()) {
                continue;
            }
            names.add(user.getName());
        }
        return names;
    }

    public List<String> getServerNames() {
        List<String> names = new ArrayList<>();
        for (Server server : Network.getServers()) {
            names.add(server.getName());
        }
        return names;
    }

    public List<String> getGroupNames() {
        List<String> names = new ArrayList<>();
        for (Group group : Network.getGroups()) {
            names.add(group.getName());
        }
        return names;
    }

    public List<String> getGameNames() {
        return new ArrayList<>(Database.getGames().getGamesName());
    }

    private class Command extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

        public Command(String name) {
            super(name);
        }

        public Command(String name, String[] aliases) {
            super(name, null, aliases);
        }

        @Override
        public void execute(net.md_5.bungee.api.CommandSender cmdSender, String[] args) {
            if (commands.containsKey(super.getName())) {
                ExCommand<Sender, Argument> basicCmd = commands.get(super.getName());
                Sender sender = new Sender(new CommandSender(cmdSender), basicCmd.getPlugin());
                LinkedList<Argument> extendedArgs = new LinkedList<>();
                for (String arg : args) {
                    extendedArgs.addLast(new Argument(sender, arg));
                }
                basicCmd.getListener().onCommand(sender, basicCmd, new Arguments<>(sender, extendedArgs));
            }
        }

        @Override
        public Iterable<String> onTabComplete(net.md_5.bungee.api.CommandSender cmdSender, String[] args) {
            if (commands.containsKey(super.getName())) {
                ExCommand<Sender, Argument> basicCmd = commands.get(super.getName());
                Sender sender = new Sender(new CommandSender(cmdSender), basicCmd.getPlugin());
                LinkedList<Argument> extendedArgs = new LinkedList<>();
                for (String arg : args) {
                    extendedArgs.add(new Argument(sender, arg));
                }
                List<String> tab = basicCmd.getListener().getTabCompletion(basicCmd, new Arguments<>(sender,
                        extendedArgs));
                return tab != null ? tab : List.of();
            }
            return null;
        }

    }
}