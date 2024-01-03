/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.chat;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.DisplayGroup;
import de.timesnake.library.extension.util.permission.PermGroup;

import java.util.Collection;
import java.util.List;

public class Completion extends de.timesnake.library.commands.Completion<Completion, Sender, Argument, Arguments<Argument>> {

  public static Completion empty() {
    return new Completion(null, List.of());
  }

  public static Completion ofPlayerNames() {
    return new Completion(Network.getUsers().stream().filter(u -> !u.isAirMode()).map(User::getName).toList());
  }

  public static Completion ofGameNames() {
    return new Completion(Database.getGames().getGamesName());
  }

  public static Completion ofServerNames() {
    return new Completion(Network.getServers().stream().map(Server::getName).toList());
  }

  public static Completion ofPermGroupNames() {
    return new Completion(Network.getGroupManager().getPermGroups().values().stream().map(PermGroup::getName).toList());
  }

  public static Completion ofDisplayGroupNames() {
    return new Completion(Network.getGroupManager().getDisplayGroups().values().stream().map(DisplayGroup::getName).toList());
  }

  public Completion() {
    super();
  }

  public Completion(Code permission) {
    super(permission);
  }

  public Completion(Collection<String> values) {
    super(values);
  }

  public Completion(CmdFunction<Sender, Argument, Arguments<Argument>, Collection<String>> valuesProvider) {
    super(valuesProvider);
  }

  public Completion(String... values) {
    this(List.of(values));
  }

  public Completion(Code permission, Collection<String> values) {
    super(permission, values);
  }

}
