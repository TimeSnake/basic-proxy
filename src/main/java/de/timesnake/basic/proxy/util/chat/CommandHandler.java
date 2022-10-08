/*
 * basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.basic.proxy.util.chat;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import de.timesnake.basic.proxy.core.group.DisplayGroup;
import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler {

    private final HashMap<String, ExCommand<Sender, Argument>> commands = new HashMap<>();

    public void addCommand(Object mainClass, String cmd, CommandListener<Sender, Argument> listener,
                           Plugin basicPlugin) {
        listener.loadCodes(basicPlugin);
        this.commands.put(cmd, new ExCommand<>(cmd, listener, basicPlugin));
        Command command = new Command();
        CommandMeta commandMeta = BasicProxy.getCommandManager().metaBuilder(cmd).plugin(mainClass).build();
        BasicProxy.getCommandManager().register(commandMeta, command);
    }

    public void addCommand(Object mainClass, String cmd, List<String> aliases,
                           CommandListener<Sender, Argument> listener, Plugin basicPlugin) {
        listener.loadCodes(basicPlugin);
        ExCommand<Sender, Argument> exCommand = new ExCommand<>(cmd, listener, basicPlugin);
        this.commands.put(cmd, exCommand);

        for (String alias : aliases) {
            this.commands.put(alias, exCommand);
        }

        Command command = new Command();
        CommandMeta commandMeta =
                BasicProxy.getCommandManager().metaBuilder(cmd).aliases(aliases.toArray(new String[0])).plugin(mainClass).build();
        BasicProxy.getCommandManager().register(commandMeta, command);
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

    @Deprecated
    public List<String> getGroupNames() {
        return this.getPermGroupNames();
    }

    public List<String> getPermGroupNames() {
        return Network.getPermGroups().stream().map(PermGroup::getName).collect(Collectors.toList());
    }

    public List<String> getDisplayGroupNames() {
        return Network.getDisplayGroups().stream().map(DisplayGroup::getName).collect(Collectors.toList());
    }

    public List<String> getGameNames() {
        return new ArrayList<>(Database.getGames().getGamesName());
    }

    private class Command implements SimpleCommand {

        public Command() {

        }

        @Override
        public List<String> suggest(final Invocation invocation) {
            if (commands.containsKey(invocation.alias())) {
                ExCommand<Sender, Argument> basicCmd = commands.get(invocation.alias());
                Sender sender = new Sender(new CommandSender(invocation.source()), basicCmd.getPlugin());
                LinkedList<Argument> extendedArgs = new LinkedList<>();
                for (String arg : invocation.arguments()) {
                    extendedArgs.add(new Argument(sender, arg));
                }
                List<String> tab = basicCmd.getListener().getTabCompletion(basicCmd, new Arguments<>(sender,
                        extendedArgs));
                return tab != null ? tab : List.of();
            }
            return List.of();
        }

        @Override
        public void execute(Invocation invocation) {
            if (commands.containsKey(invocation.alias())) {
                ExCommand<Sender, Argument> basicCmd = commands.get(invocation.alias());
                Sender sender = new Sender(new CommandSender(invocation.source()), basicCmd.getPlugin());
                LinkedList<Argument> extendedArgs = new LinkedList<>();
                for (String arg : invocation.arguments()) {
                    extendedArgs.addLast(new Argument(sender, arg));
                }
                basicCmd.getListener().onCommand(sender, basicCmd, new Arguments<>(sender, extendedArgs));
            }
        }
    }
}