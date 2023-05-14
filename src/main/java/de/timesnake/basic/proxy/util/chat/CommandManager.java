/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.chat;

import com.velocitypowered.api.command.SimpleCommand;
import de.timesnake.basic.proxy.core.group.DisplayGroup;
import de.timesnake.basic.proxy.core.group.PermGroup;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.ArgumentParseException;
import de.timesnake.library.extension.util.cmd.CommandExitException;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.CommandListenerBasis;
import de.timesnake.library.extension.util.cmd.DuplicateOptionException;
import de.timesnake.library.extension.util.cmd.ExCommand;
import de.timesnake.library.extension.util.cmd.IncCommandContext;
import de.timesnake.library.extension.util.cmd.IncCommandListener;
import de.timesnake.library.extension.util.cmd.IncCommandOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;

public class CommandManager {

    private final HashMap<String, ExCommand<Sender, Argument>> commands = new HashMap<>();
    private final HashMap<String, IncCommandContext> incCmdContexts = new HashMap<>();

    public void addCommand(Object mainClass, String cmd,
            CommandListenerBasis<Sender, Argument> listener, Plugin basicPlugin) {
        listener.loadCodes(basicPlugin);

        ExCommand<Sender, Argument> exCommand = new ExCommand<>(cmd, listener, basicPlugin);
        this.commands.put(cmd, exCommand);

        BasicProxy.getCommandManager().register(BasicProxy.getCommandManager()
                .metaBuilder(cmd).plugin(mainClass).build(), new Command());
    }

    public void addCommand(Object mainClass, String cmd, List<String> aliases,
            CommandListener<Sender, Argument> listener, Plugin basicPlugin) {
        listener.loadCodes(basicPlugin);
        ExCommand<Sender, Argument> exCommand = new ExCommand<>(cmd, listener, basicPlugin);
        this.commands.put(cmd, exCommand);

        for (String alias : aliases) {
            this.commands.put(alias, exCommand);
        }

        BasicProxy.getCommandManager().register(BasicProxy.getCommandManager().metaBuilder(cmd)
                .aliases(aliases.toArray(new String[0])).plugin(mainClass).build(), new Command());
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
        return Network.getGroupManager().getPermGroups().values().stream().map(PermGroup::getName)
                .collect(Collectors.toList());
    }

    public List<String> getDisplayGroupNames() {
        return Network.getGroupManager().getDisplayGroups().values().stream()
                .map(DisplayGroup::getName)
                .collect(Collectors.toList());
    }

    public List<String> getGameNames() {
        return new ArrayList<>(Database.getGames().getGamesName());
    }

    public static class Arguments extends
            de.timesnake.library.extension.util.cmd.Arguments<Argument> {

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
        public Argument createArgument(de.timesnake.library.extension.util.cmd.Sender sender,
                String arg) {
            return new Argument((Sender) sender, arg);
        }
    }

    public static class ExArguments extends
            de.timesnake.library.extension.util.cmd.ExArguments<Argument> {

        public ExArguments(de.timesnake.library.extension.util.cmd.Sender sender, String[] args,
                boolean allowDuplicateOptions) {
            super(sender, args, allowDuplicateOptions);
        }

        @Override
        public Argument createArgument(de.timesnake.library.extension.util.cmd.Sender sender,
                String arg) {
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
                Sender sender = new Sender(new CommandSender(invocation.source()),
                        basicCmd.getPlugin());
                String cmdName = invocation.alias().toLowerCase();
                String[] args = invocation.arguments();

                List<String> suggestions = List.of();

                if (basicCmd.getListener() instanceof CommandListener listener) {
                    suggestions = listener.getTabCompletion(basicCmd, new Arguments(sender, args));
                } else if (basicCmd.getListener() instanceof ExCommandListener listener) {
                    suggestions = listener.getTabCompletion(basicCmd,
                            new ExArguments(sender, args, listener.allowDuplicates(cmdName, args)));
                }

                if (suggestions != null) {
                    return suggestions;
                }
            }
            return List.of();
        }

        @Override
        public void execute(Invocation invocation) {
            if (!commands.containsKey(invocation.alias())) {
                return;
            }

            ExCommand<Sender, Argument> basicCmd = commands.get(invocation.alias());
            Sender sender = new Sender(new CommandSender(invocation.source()),
                    basicCmd.getPlugin());
            String cmdName = invocation.alias().toLowerCase();
            String[] args = invocation.arguments();

            try {
                if (basicCmd.getListener() instanceof CommandListener listener) {
                    listener.onCommand(sender, basicCmd, new Arguments(sender, args));
                } else if (basicCmd.getListener() instanceof ExCommandListener listener) {
                    listener.onCommand(sender, basicCmd, new ExArguments(sender, args,
                            listener.allowDuplicates(cmdName, args)));
                } else if (basicCmd.getListener() instanceof IncCommandListener listener) {
                    if (args.length > 0) {
                        IncCommandContext context = incCmdContexts.get(sender.getName());
                        IncCommandOption option = (IncCommandOption) listener.getOptions().stream()
                                .filter(o -> ((IncCommandOption) o).getName().equals(args[0]))
                                .findFirst().get();

                        if (context == null || option == null) {
                            return;
                        }

                        Object value = option.parseValue(args[1]);
                        context.addOption(option, value);

                        boolean finished = listener.onUpdate(sender, context, option, value);

                        if (finished) {
                            incCmdContexts.remove(sender.getName());
                        }
                    } else {
                        IncCommandContext context = listener.onCommand(sender, basicCmd,
                                new Arguments(sender, args));

                        if (context != null) {
                            incCmdContexts.put(sender.getName(), context);
                        }

                    }
                }
            } catch (CommandExitException ignored) {

            } catch (ArgumentParseException | DuplicateOptionException e) {
                sender.sendPluginMessage(Component.text(e.getMessage(), ExTextColor.WARNING));
            }
        }
    }
}