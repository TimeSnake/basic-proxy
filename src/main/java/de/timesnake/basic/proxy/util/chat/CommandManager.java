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
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.CommandHandler;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.basis.CommandListenerBasis;
import de.timesnake.library.commands.extended.ExArguments;
import de.timesnake.library.commands.simple.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager extends CommandHandler<Sender, Argument, Arguments<Argument>, ExArguments<Argument>> {

  public void addCommand(Object mainClass, String cmd, CommandListenerBasis listener, Plugin basicPlugin) {
    this.addCommand(cmd, listener, basicPlugin);

    BasicProxy.getCommandManager().register(BasicProxy.getCommandManager()
        .metaBuilder(cmd).plugin(mainClass).build(), new Command());
  }

  public void addCommand(Object mainClass, String cmd, List<String> aliases, CommandListenerBasis listener, Plugin basicPlugin) {
    this.addCommand(cmd, aliases, listener, basicPlugin);

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

  @Deprecated
  public List<String> getPermGroupNames() {
    return Network.getGroupManager().getPermGroups().values().stream().map(PermGroup::getName)
        .collect(Collectors.toList());
  }

  @Deprecated
  public List<String> getDisplayGroupNames() {
    return Network.getGroupManager().getDisplayGroups().values().stream()
        .map(DisplayGroup::getName)
        .collect(Collectors.toList());
  }

  @Deprecated
  public List<String> getGameNames() {
    return new ArrayList<>(Database.getGames().getGamesName());
  }

  @Override
  public Sender createSender(de.timesnake.library.commands.CommandSender sender, PluginCommand cmd) {
    return new Sender(((CommandSender) sender), cmd.getPlugin());
  }

  @Override
  public de.timesnake.library.commands.simple.Arguments<Argument> createArguments(Sender sender, String[] args) {
    return new Arguments(sender, args);
  }

  @Override
  public de.timesnake.library.commands.extended.ExArguments<Argument> createExArguments(Sender sender, String[] args, boolean allowDuplicates) {
    return new ExArguments(sender, args, allowDuplicates);
  }

  private static class Arguments extends de.timesnake.library.commands.simple.Arguments<Argument> {

    public Arguments(Sender sender, String[] args) {
      super(sender, args);
    }

    @Override
    public Argument createArgument(de.timesnake.library.commands.Sender sender, String arg) {
      return new Argument((Sender) sender, arg);
    }
  }

  private static class ExArguments extends de.timesnake.library.commands.extended.ExArguments<Argument> {

    public ExArguments(de.timesnake.library.commands.Sender sender, String[] args, boolean allowDuplicateOptions) {
      super(sender, args, allowDuplicateOptions);
    }

    @Override
    public Argument createArgument(de.timesnake.library.commands.Sender sender, String arg) {
      return new Argument(((Sender) sender), arg);
    }
  }

  private class Command implements SimpleCommand {

    public Command() {

    }

    @Override
    public boolean hasPermission(Invocation invocation) {
      return CommandManager.this.showCommand(new CommandSender(invocation.source()), invocation.alias().toLowerCase());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
      String[] args = invocation.arguments();
      return CommandManager.this.handleTabCompletion(new CommandSender(invocation.source()),
          invocation.alias().toLowerCase(), args, args.length > 0 ? args.length : 1);
    }

    @Override
    public void execute(Invocation invocation) {
      CommandManager.this.handleCommand(new CommandSender(invocation.source()), invocation.alias().toLowerCase(), invocation.arguments());
    }
  }
}