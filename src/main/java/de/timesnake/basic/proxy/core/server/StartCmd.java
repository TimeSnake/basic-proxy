/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.CommandListener;
import de.timesnake.basic.proxy.util.chat.Completion;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.*;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbNonTmpGame;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Availability;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.chat.Chat;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.NetworkServer.CopyType;
import de.timesnake.library.network.ServerCreationResult;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.timesnake.library.chat.ExTextColor.*;
import static net.kyori.adventure.text.Component.text;

public class StartCmd implements CommandListener {

  private final Code serverPerm = Plugin.NETWORK.createPermssionCode("network.start.server");
  private final Code gamePerm = Plugin.NETWORK.createPermssionCode("network.start.game");
  private final Code ownGamePerm = Plugin.NETWORK.createPermssionCode("network.start.own_game");
  private final Code publicGamePerm = Plugin.NETWORK.createPermssionCode("network.start.public_game");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    if (args.isLengthHigherEquals(2, true)) {
      switch (args.get(0).toLowerCase()) {
        case "server" -> {
          sender.hasPermissionElseExit(this.serverPerm);
          this.handleStartServer(sender, args);
        }
        case "own_game" -> {
          sender.hasPermissionElseExit(this.ownGamePerm);
          this.handleStartOwnGameServer(sender, args);
        }
        case "public_game" -> {
          sender.hasPermissionElseExit(this.publicGamePerm);
          this.handleStartPublicGameServer(sender, args);
        }
        case "game" -> {
          sender.hasPermissionElseExit(this.gamePerm);
          this.handleStartGame(sender, args);
        }
      }
    }
  }

  private void handleStartServer(Sender sender, Arguments<Argument> args) {
    String serverName = args.get(1).toLowerCase();
    Server server = Network.getServer(serverName);

    if (server == null) {
      sender.sendPluginTDMessage("§wServer §v" + serverName + "§w does not exist");
      return;
    }

    Status.Server status = server.getStatus();
    if (status.equals(Status.Server.LAUNCHING) || status.equals(Status.Server.LOADING)) {
      sender.sendPluginTDMessage("§wServer §v" + serverName + "§w is already starting");
      return;
    }

    if (status.isRunning()) {
      sender.sendPluginTDMessage("§wServer §v" + serverName + "§w is already running");
      return;
    }

    Integer maxPlayers = null;
    if (args.isLengthEquals(3, false) && args.get(2).isInt(false)) {
      maxPlayers = args.get(2).toInt();
    } else if (server instanceof GameServer) {
      String task = ((TaskServer) server).getTask();
      maxPlayers = Database.getGames().getGame(task).getInfo().getMaxPlayers();
    } else if (server.getType().equals(ServerType.LOBBY)) {
      maxPlayers = Network.getMaxPlayersLobby();
    } else if (server.getType().equals(ServerType.BUILD)) {
      maxPlayers = Network.getMaxPlayersBuild();
    }
    if (maxPlayers != null) {
      server.setMaxPlayers(maxPlayers);
      Network.getBukkitCmdHandler().handleServerCmd(sender, server);
    } else {
      sender.sendPluginTDMessage("§wNo default max-players value found");
    }
  }

  private void handleStartOwnGameServer(Sender sender, Arguments<Argument> args) {
    sender.isPlayerElseExit(true);

    User user = sender.getUser();
    String gameType = args.get(1).toLowerCase();

    DbGame game = Database.getGames().getGame(gameType);
    if (game == null || !game.exists()) {
      sender.sendMessageGameNotExist(gameType);
      return;
    }

    game = game.toLocal();

    if (!(game instanceof DbNonTmpGame nonTmpGame)) {
      sender.sendPluginTDMessage("§wUnsupported game type");
      return;
    }

    if (!nonTmpGame.isOwnable()) {
      sender.sendPluginTDMessage("§wServers of this game can not have an owner");
      return;
    }

    args.isLengthEqualsElseExit(3, true);

    Collection<String> serverNames = Network.getNetworkUtils().getOwnerServerNames(user.getUniqueId(),
            ServerType.GAME, ((DbNonTmpGame) game).getName());

    String serverName = args.getString(2);

    if (!serverNames.contains(serverName)) {
      sender.sendMessageServerNameNotExist(serverName);
      return;
    }

    int port = Network.nextEmptyPort();
    NetworkServer networkServer = new NetworkServer(user.getUniqueId().hashCode() + "_" + serverName, port,
        ServerType.GAME)
        .setFolderName(serverName)
        .setTask(((DbNonTmpGame) game).getName())
        .setMaxPlayers(20)
        .applyServerOptions(game.getServerOptions());

    sender.sendPluginTDMessage("§sLoading server §v" + serverName);
    Tuple<ServerCreationResult, Optional<Server>> result = Network.loadPlayerServer(user.getUniqueId(), networkServer);

    if (!result.getA().isSuccessful()) {
      sender.sendPluginTDMessage("§wError while loading server (" +
                                 ((ServerCreationResult.Fail) result.getA()).getReason() + ")");
      return;
    }

    sender.sendPluginTDMessage("§sLoaded server §v" + serverName + "§s, you will be moved in a few moments");

    NonTmpGameServer server = (NonTmpGameServer) result.getB().get();
    server.setMaxPlayers(((DbNonTmpGame) game).getMaxPlayers());
    server.setOwnerUuid(user.getUniqueId());

    Network.getBukkitCmdHandler().handleServerCmd(sender, server);
    server.addWaitingUser(user);
  }

  private void handleStartPublicGameServer(Sender sender, Arguments<Argument> args) {
    String gameType = args.get(1).toLowerCase();

    // check game
    DbGame game = Database.getGames().getGame(gameType);
    if (game == null || !game.exists()) {
      sender.sendMessageGameNotExist(gameType);
      return;
    }

    game = game.toLocal();

    if (!(game instanceof DbNonTmpGame)) {
      sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
      return;
    }

    args.isLengthEqualsElseExit(3, true);

    Collection<String> serverNames = Network.getNetworkUtils()
        .getPublicPlayerServerNames(ServerType.GAME, ((DbNonTmpGame) game).getName());

    String serverName = args.getString(2);

    if (!serverNames.contains(serverName)) {
      sender.sendMessageServerNameNotExist(serverName);
      return;
    }

    int port = Network.nextEmptyPort();
    NetworkServer networkServer = new NetworkServer(serverName, port, ServerType.GAME)
        .setTask(((DbNonTmpGame) game).getName())
        .setMaxPlayers(20)
        .applyServerOptions(game.getServerOptions());

    sender.sendPluginTDMessage("§sLoading server §v" + serverName);
    Tuple<ServerCreationResult, Optional<Server>> result = Network.loadPublicPlayerServer(networkServer);

    if (!result.getA().isSuccessful()) {
      sender.sendPluginTDMessage("§wError while loading server (" +
                                 ((ServerCreationResult.Fail) result.getA()).getReason() + ")");
      return;
    }

    sender.sendPluginTDMessage("§sLoaded server §v" + serverName + "§s, you will be moved in a few moments");

    NonTmpGameServer server = (NonTmpGameServer) result.getB().get();
    server.setMaxPlayers(((DbNonTmpGame) game).getMaxPlayers());

    Network.getBukkitCmdHandler().handleServerCmd(sender, server);

    if (sender.isPlayer(false)) {
      server.addWaitingUser(sender.getUser());
    }
  }

  private void handleStartGame(Sender sender, Arguments<Argument> args) {
    String gameType = args.get(1).toLowerCase();

    // check game
    DbGame game = Database.getGames().getGame(gameType);
    if (game == null || !game.exists()) {
      sender.sendMessageGameNotExist(gameType);
      return;
    }

    game = game.toLocal();

    if (game instanceof DbTmpGame) {
      this.handleStartTmpGame(sender, args, (DbTmpGame) game);
    } else {
      this.handleStartNonTmpGame(sender, args, (DbNonTmpGame) game);
    }
  }

  private void handleStartNonTmpGame(Sender sender, Arguments<Argument> args, DbNonTmpGame game) {
    if (game.isOwnable()) {
      sender.sendPluginTDMessage("§wUnsupported game type");
      return;
    }

    String gameName = game.getName();
    Integer gameMaxPlayers = game.getMaxPlayers();

    Availability gameMapAvailability = game.getMapAvailability();

    boolean mapsEnabled = args.getArgumentByString("maps") != null || gameMapAvailability.equals(Availability.REQUIRED);

    if (gameMapAvailability.equals(Availability.FORBIDDEN) && mapsEnabled) {
      sender.sendPluginTDMessage("§wGame §v" + gameName + "§w forbids maps");
      return;
    }

    boolean oldPvP = args.getArgumentByString("oldpvp") != null || args.getArgumentByString("1.8pvp") != null;

    sender.sendPluginTDMessage("§sCreating server...");

    Network.runTaskAsync(() -> {
      int port = Network.nextEmptyPort();
      NetworkServer networkServer = new NetworkServer((port % 1000) + gameName + Network.TMP_SERVER_SUFFIX, port,
          ServerType.GAME)
          .setTask(gameName)
          .options(o -> o.setWorldCopyType(mapsEnabled ? CopyType.COPY : CopyType.NONE))
          .applyServerOptions(game.getServerOptions());

      Tuple<ServerCreationResult, Optional<Server>> result = Network.createTmpServer(networkServer);
      if (!result.getA().isSuccessful()) {
        sender.sendPluginTDMessage("§wError while creating a game server! Please contact an administrator ("
                                   + ((ServerCreationResult.Fail) result.getA()).getReason() + ")");
        return;
      }

      NonTmpGameServer server = (NonTmpGameServer) result.getB().get();

      server.setTaskSynchronized(gameName);
      server.setMaxPlayers(gameMaxPlayers);

      sender.sendPluginTDMessage("§sStarted game §v" + gameName);
      sender.sendPluginTDMessage("§sGame server: §v" + server.getName());
      sender.sendPluginTDMessage("§sMax players: §v" + gameMaxPlayers);
      sender.sendPluginTDMessage("$sOld PvP: §v" + oldPvP);

      Network.getBukkitCmdHandler().handleServerCmd(sender, server);
    });
  }

  private void handleStartTmpGame(Sender sender, Arguments<Argument> args, DbTmpGame game) {
    String gameName = game.getName();
    Availability gameKitAvailability = game.getKitAvailability();
    Availability gameMapAvailability = game.getMapAvailability();
    Collection<Integer> gameTeamAmounts = game.getTeamSizes();
    Integer gameMaxPlayers = game.getMaxPlayers();
    Integer gameMinPlayers = game.getMinPlayerNumber();
    Availability gameMergeTeams = game.getTeamMergeAvailability();

    // kits
    boolean kitsEnabled = args.getArgumentByString("kits") != null || gameKitAvailability.equals(Availability.REQUIRED);

    if (gameKitAvailability.equals(Availability.FORBIDDEN) && kitsEnabled) {
      sender.sendPluginMessage(text("Game ", WARNING)
          .append(text(gameName, VALUE))
          .append(text(" forbid kits", WARNING)));
      return;
    }

    // maps
    boolean mapsEnabled = args.getArgumentByString("maps") != null || gameMapAvailability.equals(Availability.REQUIRED);

    if (gameMapAvailability.equals(Availability.FORBIDDEN) && mapsEnabled) {
      sender.sendPluginMessage(text("Game ", WARNING)
          .append(text(gameName, VALUE))
          .append(text(" forbid maps", WARNING)));
      return;
    }

    // check player amount
    Integer maxServerPlayers = gameMaxPlayers;
    Argument argMaxPlayers = args.getArgumentByStringPart("max=");
    if (argMaxPlayers != null) {
      String stringMax = argMaxPlayers.getString().replace("max=", "");
      try {
        maxServerPlayers = Integer.parseInt(stringMax);
      } catch (NumberFormatException e) {
        sender.sendPluginMessage(text("Invalid max player amount", WARNING));
        return;
      }

      if (maxServerPlayers > gameMaxPlayers) {
        sender.sendPluginMessage(text("Too large max players amount for game ", WARNING)
            .append(text(gameName, VALUE))
            .append(text(", max is ", WARNING))
            .append(text(gameMaxPlayers, VALUE)));
        return;
      }

      if (maxServerPlayers < gameMinPlayers) {
        sender.sendPluginMessage(text("Too small max players amount for game ", WARNING)
            .append(text(gameName, VALUE))
            .append(text(", min is ", WARNING))
            .append(text(gameMinPlayers, VALUE)));
        return;
      }
    }

    Integer teamAmount = null;

    // check players per team
    Integer playersPerTeam = null;
    Argument argMaxPerTeam = args.getArgumentByStringPart("mppt=");
    if (argMaxPerTeam != null) {
      String stringMaxPerTeam = argMaxPerTeam.getString().replace("mppt=", "");
      try {
        playersPerTeam = Integer.parseInt(stringMaxPerTeam);
      } catch (NumberFormatException e) {
        sender.sendPluginMessage(text("Invalid max players per team amount", WARNING));
        return;
      }

      teamAmount = (int) Math.ceil(maxServerPlayers / ((double) playersPerTeam));
    }

    // team amount

    if (teamAmount == null) {
      teamAmount = Collections.max(gameTeamAmounts);
      Argument teamsArg = args.getArgumentByStringPart("teams=");
      if (teamsArg != null) {
        String teams = teamsArg.getString().replace("teams=", "");
        try {
          teamAmount = Integer.parseInt(teams);
        } catch (NumberFormatException e) {
          sender.sendPluginMessage(text("Invalid team amount", WARNING));
          return;
        }

        if (!gameTeamAmounts.contains(teamAmount)) {
          sender.sendPluginMessage(text("Invalid team amount", WARNING));
          sender.sendPluginMessage(text("Available team amounts: ", PERSONAL)
              .append(Chat.listToComponent(gameTeamAmounts, VALUE, PERSONAL)));
          return;
        }
      }
    }

    boolean teamMerging = argMaxPerTeam != null && args.getArgumentByString("merge") != null;

    if (gameMergeTeams.equals(Availability.FORBIDDEN) && teamMerging) {
      sender.sendPluginMessage(text("Game ", WARNING)
          .append(text(gameName, VALUE))
          .append(text(" forbid team merging", WARNING)));
      return;
    }

    if (gameMergeTeams.equals(Availability.REQUIRED) && !teamMerging && teamAmount > 0) {
      sender.sendPluginMessage(text("Game ", WARNING).
          append(text(gameName, VALUE))
          .append(text(" require team merging", WARNING)));
      return;
    }

    boolean oldPvP = args.getArgumentByString("oldpvp") != null
        || args.getArgumentByString("1.8pvp") != null;

    // search temp game and lounge server

    Integer finalMaxServerPlayers = maxServerPlayers;
    Integer finalTeamAmount = teamAmount;
    Integer finalPlayersPerTeam = playersPerTeam;

    Network.runTaskAsync(() -> {
      sender.sendPluginMessage(text("Creating server...", PERSONAL));
      int loungePort = Network.nextEmptyPort();
      NetworkServer loungeNetworkServer = new NetworkServer((loungePort % 1000) +
          ServerType.LOUNGE.getShortName() + Network.TMP_SERVER_SUFFIX, loungePort,
          ServerType.LOUNGE)
          .options(o -> o.setWorldCopyType(CopyType.COPY));

      Tuple<ServerCreationResult, Optional<Server>> loungeResult =
          Network.createTmpServer(loungeNetworkServer);

      if (!loungeResult.getA().isSuccessful()) {
        sender.sendPluginMessage(text("Error while creating a" + " lounge server! " +
                "Please contact an administrator ("
                + ((ServerCreationResult.Fail) loungeResult.getA()).getReason() + ")",
            WARNING));
        return;
      }

      int tempGamePort = Network.nextEmptyPort();

      NetworkServer gameNetworkServer = new NetworkServer(
          (tempGamePort % 1000) + gameName + Network.TMP_SERVER_SUFFIX,
          tempGamePort, ServerType.TEMP_GAME).setTask(
              gameName)
          .options(o -> o.setWorldCopyType(mapsEnabled ? CopyType.COPY : CopyType.NONE))
          .applyServerOptions(game.getServerOptions());

      Tuple<ServerCreationResult, Optional<Server>> tempServerResult = Network.createTmpServer(
          gameNetworkServer);
      if (!tempServerResult.getA().isSuccessful()) {
        sender.sendPluginMessage(text("Error while creating a" + " " + gameName
                + " server! Please contact an administrator (" +
                ((ServerCreationResult.Fail) tempServerResult.getA()).getReason() + ")",
            WARNING));
        return;
      }

      LoungeServer loungeServer = (LoungeServer) loungeResult.getB().get();
      TmpGameServer tmpGameServer = ((TmpGameServer) tempServerResult.getB().get());

      loungeServer.setTaskSynchronized(gameName);
      loungeServer.setMaxPlayers(finalMaxServerPlayers);

      tmpGameServer.setTaskSynchronized(gameName);
      tmpGameServer.setMapsEnabled(mapsEnabled);
      tmpGameServer.setKitsEnabled(kitsEnabled);
      tmpGameServer.setMaxPlayers(finalMaxServerPlayers);
      tmpGameServer.setTeamAmount(finalTeamAmount);
      tmpGameServer.setMapsEnabled(mapsEnabled);
      tmpGameServer.setTeamMerging(teamMerging);
      tmpGameServer.setMaxPlayersPerTeam(finalPlayersPerTeam);
      tmpGameServer.setPvP(oldPvP);
      tmpGameServer.setTwinServer((DbLoungeServer) loungeServer.getDatabase());

      sender.sendPluginTDMessage("§sStarted game §v" + gameName);
      sender.sendPluginTDMessage("§sGame server: §v" + tmpGameServer.getName());
      sender.sendPluginTDMessage("§sLounge server: §v" + loungeServer.getName());
      sender.sendPluginTDMessage("§sMax players: §v" + finalMaxServerPlayers);
      sender.sendPluginTDMessage("§sMaps: §v" + mapsEnabled);
      sender.sendPluginTDMessage("§sKits: §v" + kitsEnabled);
      sender.sendPluginTDMessage("§sTeam amount: §v" + finalTeamAmount);
      sender.sendPluginTDMessage("§sTeam merging: §v" + teamMerging);
      sender.sendPluginTDMessage("§sTeam size: §v" + finalPlayersPerTeam);
      sender.sendPluginTDMessage("§sOld PvP: §v" + oldPvP);

      Network.getBukkitCmdHandler().handleServerCmd(sender, loungeServer);

    });
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.gamePerm)
        .addArgument(new Completion("game")
            .addArgument(Completion.ofGameNames()))
        .addArgument(new Completion("server")
            .addArgument(Completion.ofServerNames()))
        .addArgument(new Completion("own_game"))
        .addArgument(new Completion("public_game")
            .addArgument((sender, cmd, args) -> Database.getGames().containsGame(args.getString(1)),
                new Completion((sender, cmd, args) -> Database.getGames().containsGame(args.getString(1)) ?
                    Network.getNetworkUtils().getPublicPlayerServerNames(ServerType.GAME, args.getString(1)) : List.of())));
  }

  @Override
  public String getPermission() {
    return this.gamePerm.getPermission();
  }
}
