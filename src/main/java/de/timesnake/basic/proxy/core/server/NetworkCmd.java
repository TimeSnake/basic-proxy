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

  private final Code perm = Plugin.INFO.createPermssionCode("network.create.save");
  private final Code serverAlreadyExists = Plugin.NETWORK.createHelpCode("Save name already exists");
  private final Code createOwnPerm = Plugin.NETWORK.createPermssionCode("network.create.save.private");
  private final Code createPublicGame = Plugin.NETWORK.createPermssionCode("network.create.save.public");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    args.isLengthHigherEqualsElseExit(1, true);

    switch (args.getString(0).toLowerCase()) {
      case "create_private_save" -> this.handleCreatePrivateGameSaveCmd(sender, args);
      case "create_public_save" -> this.handleCreatePublicGameSaveCmd(sender, args);
    }
  }

  private void handleCreatePublicGameSaveCmd(Sender sender, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.createPublicGame);
    args.isLengthHigherEqualsElseExit(3, true);

    DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

    if (!(game instanceof DbNonTmpGame nonTmpGame)) {
      sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
      return;
    }

    Collection<String> serverNames = Network.getNetworkUtils().getPublicSaveNames(ServerType.GAME,
        nonTmpGame.getName());

    String saveName = args.get(2).toLowerCase();

    if (serverNames.contains(saveName)) {
      sender.sendMessageAlreadyExist(saveName, this.serverAlreadyExists, "save");
      return;
    }

    ServerInitResult result = Network.getServerManager().createPublicSave(ServerType.GAME,
        ((DbNonTmpGame) game).getName(),
        saveName);

    if (!result.isSuccessful()) {
      sender.sendPluginTDMessage("§wError while creating save (" + ((ServerInitResult.Fail) result).getReason() +
                                 ")");
      return;
    }

    sender.sendPluginTDMessage("§sCreated save §v" + saveName);
  }

  private void handleCreatePrivateGameSaveCmd(Sender sender, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.createOwnPerm);
    args.isLengthEqualsElseExit(4, true);

    DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

    if (!(game instanceof DbNonTmpGame nonTmpGame)) {
      sender.sendPluginTDMessage("§wUnsupported game type");
      return;
    }

    if (!nonTmpGame.isOwnable()) {
      sender.sendPluginTDMessage("§wSaves of this game can not have an owner");
      return;
    }

    Argument playerArg = args.get(3);

    if (!playerArg.isPlayerDatabaseName(true)) {
      return;
    }

    DbUser user = playerArg.toDbUser();

    Collection<String> serverNames = Network.getNetworkUtils().getPrivateSaveNames(user.getUniqueId(), ServerType.GAME,
        ((DbNonTmpGame) game).getName());

    String saveName = args.get(2).toLowerCase();

    if (serverNames.contains(user.getUniqueId().hashCode() + saveName)) {
      sender.sendMessageAlreadyExist(saveName, this.serverAlreadyExists, "save");
      return;
    }

    ServerInitResult result = Network.getServerManager().createPrivateSave(user.getUniqueId(), ServerType.GAME,
        ((DbNonTmpGame) game).getName(), user.getUniqueId().hashCode() + saveName);

    if (!result.isSuccessful()) {
      sender.sendPluginTDMessage("§wError while creating save §v" + saveName + "§w: §v" +
                                 ((ServerInitResult.Fail) result).getReason());
      return;
    }

    sender.sendPluginTDMessage("§sCreated save §v" + saveName + "§q (" + user.getUniqueId().hashCode() + saveName +
                               ")");
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
