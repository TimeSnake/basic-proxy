/*
 * Copyright (C) 2022 timesnake
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
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.ArgumentParseException;
import de.timesnake.library.extension.util.cmd.CommandExitException;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.DuplicateOptionException;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;

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

    public static class Arguments extends de.timesnake.library.extension.util.cmd.Arguments<Argument> {

        public Arguments(Sender sender, String[] args) {
            super(sender, args);
        }

        public Arguments(Sender sender, LinkedList<Argument> args) {
            super(sender, args);
        }

        public Arguments(de.timesnake.library.extension.util.cmd.Arguments<Argument> args) {
            super(args);
        }

        public Arguments(Sender sender, Argument... args) {
            super(sender, args);
        }

        @Override
        public Argument createArgument(de.timesnake.library.extension.util.cmd.Sender sender, String arg) {
            return new Argument((Sender) sender, arg);
        }
    }

    public static class ExArguments extends de.timesnake.library.extension.util.cmd.ExArguments<Argument> {

        public ExArguments(de.timesnake.library.extension.util.cmd.Sender sender, String[] args,
                           boolean allowDuplicateOptions) {
            super(sender, args, allowDuplicateOptions);
        }

        @Override
        public Argument createArgument(de.timesnake.library.extension.util.cmd.Sender sender, String arg) {
            return new Argument(((Sender) sender), arg);
        }
    }

    private class Command implements SimpleCommand {

        public Command() {

        }

        @Override
        public List<String> suggest(final Invocation invocation) {
            if (commands.containsKey(invocation.alias())) {
                ExCommand<Sender, Argument> basicCmd = commands.get(invocation.alias());
                Sender sender = new Sender(new CommandSender(invocation.source()), basicCmd.getPlugin());
                String cmdName = invocation.alias().toLowerCase();
                String[] args = invocation.arguments();

                List<String> suggestions = switch (basicCmd.getListener().getArgumentType(cmdName, args)) {
                    case DEFAULT -> basicCmd.getListener().getTabCompletion(basicCmd, new Arguments(sender, args));
                    case EXTENDED -> basicCmd.getListener().getTabCompletion(basicCmd, new ExArguments(sender, args,
                            basicCmd.getListener().allowDuplicates(cmdName, args)));
                };
                if (suggestions != null) {
                    return suggestions;
                }
            }
            return List.of();
        }

        @Override
        public void execute(Invocation invocation) {
            if (commands.containsKey(invocation.alias())) {
                ExCommand<Sender, Argument> basicCmd = commands.get(invocation.alias());
                Sender sender = new Sender(new CommandSender(invocation.source()), basicCmd.getPlugin());
                String cmdName = invocation.alias().toLowerCase();
                String[] args = invocation.arguments();

                try {
                    switch (basicCmd.getListener().getArgumentType(cmdName, args)) {
                        case DEFAULT -> basicCmd.getListener().onCommand(sender, basicCmd, new Arguments(sender, args));
                        case EXTENDED ->
                                basicCmd.getListener().onCommand(sender, basicCmd, new ExArguments(sender, args,
                                        basicCmd.getListener().allowDuplicates(cmdName, args)));
                    }
                } catch (CommandExitException ignored) {

                } catch (ArgumentParseException | DuplicateOptionException e) {
                    sender.sendPluginMessage(Component.text(e.getMessage(), ExTextColor.WARNING));
                }
            }
        }
    }
}