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
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.network.ServerInitResult;
import net.kyori.adventure.text.Component;

import java.util.Collection;

import static de.timesnake.library.chat.ExTextColor.WARNING;

public class NetworkCmd implements CommandListener {

  private final Code perm = Plugin.INFO.createPermssionCode("network.create.game");
  private final Code serverAlreadyExists = Plugin.NETWORK.createHelpCode("Server name already exists");
  private final Code createOwnPerm = Plugin.NETWORK.createPermssionCode("network.create.own_game");
  private final Code createPublicGame = Plugin.NETWORK.createPermssionCode("network.create.public_game");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    args.isLengthHigherEqualsElseExit(1, true);

    switch (args.getString(0).toLowerCase()) {
      case "create_own_game" -> this.handleCreateOwnGameCmd(sender, args);
      case "create_public_game" -> this.handleCreatePublicGameCmd(sender, args);
    }
  }

  private void handleCreatePublicGameCmd(Sender sender, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.createPublicGame);
    args.isLengthHigherEqualsElseExit(3, true);

    DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

    if (!(game instanceof DbNonTmpGame nonTmpGame)) {
      sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
      return;
    }

    Collection<String> serverNames = Network.getNetworkUtils().getPublicSaveNames(ServerType.GAME,
        nonTmpGame.getName());

    String serverName = args.get(2).toLowerCase();

    if (serverNames.contains(serverName)) {
      sender.sendMessageAlreadyExist(serverName, this.serverAlreadyExists, "server");
      return;
    }

    ServerInitResult result = Network.getServerManager().createPublicSave(ServerType.GAME,
        ((DbNonTmpGame) game).getName(),
        serverName);

    if (!result.isSuccessful()) {
      sender.sendPluginTDMessage("§wError while creating server (" + ((ServerInitResult.Fail) result).getReason() +
                                 ")");
      return;
    }

    sender.sendPluginTDMessage("§sCreated server §v" + serverName);
  }

  private void handleCreateOwnGameCmd(Sender sender, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.createOwnPerm);
    args.isLengthEqualsElseExit(4, true);

    DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

    if (!(game instanceof DbNonTmpGame nonTmpGame)) {
      sender.sendPluginTDMessage("§wUnsupported game type");
      return;
    }

    if (!nonTmpGame.isOwnable()) {
      sender.sendPluginTDMessage("§wServers of this game can not have an owner");
      return;
    }

    Argument playerArg = args.get(3);

    if (!playerArg.isPlayerDatabaseName(true)) {
      return;
    }

    DbUser user = playerArg.toDbUser();

    Collection<String> serverNames = Network.getNetworkUtils().getPrivateSaveNames(user.getUniqueId(), ServerType.GAME,
        ((DbNonTmpGame) game).getName());

    String serverName = args.get(2).toLowerCase();

    if (serverNames.contains(user.getUniqueId().hashCode() + serverName)) {
      sender.sendMessageAlreadyExist(serverName, this.serverAlreadyExists, "server");
      return;
    }

    ServerInitResult result = Network.getServerManager().createPrivateSave(user.getUniqueId(), ServerType.GAME,
        ((DbNonTmpGame) game).getName(), user.getUniqueId().hashCode() + serverName);

    if (!result.isSuccessful()) {
      sender.sendPluginTDMessage("§wError while creating server §v" + serverName + "§w: §v" +
                                 ((ServerInitResult.Fail) result).getReason());
      return;
    }

    sender.sendPluginTDMessage("§sCreated server §v" + serverName + "§q (" + user.getUniqueId().hashCode() + serverName + ")");
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
