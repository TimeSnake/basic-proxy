/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbNonTmpGame;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.network.ServerInitResult;
import net.kyori.adventure.text.Component;

import java.util.Collection;

import static de.timesnake.library.chat.ExTextColor.PERSONAL;
import static de.timesnake.library.chat.ExTextColor.WARNING;

public class NetworkCmd implements CommandListener {

  private final Code perm = Plugin.INFO.createPermssionCode("network.create.game");
  private final Code serverAlreadyExists = Plugin.NETWORK.createHelpCode("Server name already exists");
  private final Code createOwnPerm = Plugin.NETWORK.createPermssionCode("network.create.own_game");
  private final Code createPublicGame = Plugin.NETWORK.createPermssionCode("network.create.public_game");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd,
                        Arguments<Argument> args) {
    if (!args.isLengthHigherEquals(1, true)) {
      return;
    }

    switch (args.getString(0).toLowerCase()) {
      case "create_own_game" -> this.handleCreateOwnGameCmd(sender, args);
      case "create_public_game" -> this.handleCreatePublicGameCmd(sender, args);
    }
  }

  private void handleCreatePublicGameCmd(Sender sender, Arguments<Argument> args) {
    if (!sender.hasPermission(this.createPublicGame)) {
      return;
    }

    if (!args.isLengthHigherEquals(3, true)) {
      return;
    }

    DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

    if (!(game instanceof DbNonTmpGame nonTmpGame)) {
      sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
      return;
    }

    Collection<String> serverNames = Network.getNetworkUtils().getPublicPlayerServerNames(ServerType.GAME, nonTmpGame.getName());

    String serverName = args.get(2).toLowerCase();

    if (serverNames.contains(serverName)) {
      sender.sendMessageAlreadyExist(serverName, this.serverAlreadyExists, "server");
      return;
    }

    ServerInitResult result = Network.createPublicPlayerServer(ServerType.GAME, ((DbNonTmpGame) game).getName(), serverName);

    if (!result.isSuccessful()) {
      sender.sendPluginMessage(Component.text("Error while creating server (" + ((ServerInitResult.Fail) result).getReason() + ")", WARNING));
      return;
    }

    sender.sendPluginMessage(Component.text("Created server ", PERSONAL)
        .append(Component.text(serverName, ExTextColor.VALUE))
        .append(Component.text(" (" + serverName + ")", ExTextColor.QUICK_INFO)));
  }

  private void handleCreateOwnGameCmd(Sender sender, Arguments<Argument> args) {
    if (!sender.hasPermission(this.createOwnPerm)) {
      return;
    }

    if (!args.isLengthEquals(4, true)) {
      return;
    }

    DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

    if (!(game instanceof DbNonTmpGame nonTmpGame)) {
      sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
      return;
    }

    if (!nonTmpGame.isOwnable()) {
      sender.sendPluginMessage(
          Component.text("Servers of this game can not have an owner", WARNING));
      return;
    }

    Argument playerArg = args.get(3);

    if (!playerArg.isPlayerDatabaseName(true)) {
      return;
    }

    DbUser user = playerArg.toDbUser();

    Collection<String> serverNames = Network.getNetworkUtils()
        .getOwnerServerNames(user.getUniqueId(), ServerType.GAME,
            ((DbNonTmpGame) game).getName());

    String serverName = args.get(2).toLowerCase();

    if (serverNames.contains(user.getUniqueId().hashCode() + serverName)) {
      sender.sendMessageAlreadyExist(serverName, this.serverAlreadyExists, "server");
      return;
    }

    ServerInitResult result = Network.createPlayerServer(user.getUniqueId(), ServerType.GAME,
        ((DbNonTmpGame) game).getName(), user.getUniqueId().hashCode() + serverName);

    if (!result.isSuccessful()) {
      sender.sendPluginMessage(Component.text("Error while creating server (" +
          ((ServerInitResult.Fail) result).getReason() + ")", WARNING));
      return;
    }

    sender.sendPluginMessage(Component.text("Created server ", PERSONAL)
        .append(Component.text(serverName, ExTextColor.VALUE))
        .append(Component.text(" (" + user.getUniqueId().hashCode() + serverName + ")", ExTextColor.QUICK_INFO)));
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(new Completion("create_own_game", "create_public_game")
            .addArgument(Completion.ofGameNames()
                .addArgument(new Completion("<name>")
                    .addArgument(Completion.ofPlayerNames()))));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
