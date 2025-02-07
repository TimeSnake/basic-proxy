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
import de.timesnake.library.network.NetworkServer.CopyType;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
          this.handleLoadPrivateGameSave(sender, args);
        }
        case "public_game" -> {
          sender.hasPermissionElseExit(this.publicGamePerm);
          this.handleLoadPublicGameSave(sender, args);
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
      Network.runTaskAsync(() -> {
        boolean isStart = server.start();
        if (!isStart) {
          sender.sendPluginTDMessage("§wError while starting server §v" + server.getName());
          return;
        }
        sender.sendPluginTDMessage("§sStarted server §v" + server.getName());
      });
    } else {
      sender.sendPluginTDMessage("§wNo default max-players value found");
    }
  }

  private void handleLoadPrivateGameSave(Sender sender, Arguments<Argument> args) {
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
      sender.sendPluginTDMessage("§wSaves of this game can not have an owner");
      return;
    }

    args.isLengthEqualsElseExit(3, true);

    Collection<String> saveNames = Network.getNetworkUtils().getPrivateSaveNames(user.getUniqueId(),
            ServerType.GAME, ((DbNonTmpGame) game).getName());

    String saveName = args.getString(2);

    if (!saveNames.contains(saveName)) {
      sender.sendMessageServerNameNotExist(saveName);
      return;
    }

    DbGame finalGame = game;

    Network.runTaskAsync(() -> {
      sender.sendPluginTDMessage("§sLoading save §v" + saveName);
      ServerSetupResult result =
          Network.getServerManager().loadPrivateSaveToServer(((DbNonTmpGame) finalGame).getName(),
              user.getUniqueId(), saveName, s -> s.applyServerOptions(finalGame.getServerOptions()));

      if (!result.isSuccessful()) {
        sender.sendPluginTDMessage("§wError while loading save (" +
                                   ((ServerSetupResult.Fail) result).getReason() + ")");
        return;
      }

      sender.sendPluginTDMessage("§sLoaded save §v" + saveName + "§s, you will be moved in a few moments");

      NonTmpGameServer server = (NonTmpGameServer) ((ServerSetupResult.Success) result).getServer();
      server.setMaxPlayers(((DbNonTmpGame) finalGame).getMaxPlayers());
      server.setOwnerUuid(user.getUniqueId());

      boolean isStart = server.start();
      if (!isStart) {
        sender.sendPluginTDMessage("§wError while starting server §v" + server.getName());
        return;
      }
      sender.sendPluginTDMessage("§sStarted server §v" + server.getName());
      server.addWaitingUser(user);
    });
  }

  private void handleLoadPublicGameSave(Sender sender, Arguments<Argument> args) {
    String gameType = args.get(1).toLowerCase();

    DbGame game = Database.getGames().getGame(gameType);
    if (game == null || !game.exists()) {
      sender.sendMessageGameNotExist(gameType);
      return;
    }

    game = game.toLocal();

    if (!(game instanceof DbNonTmpGame)) {
      sender.sendPluginTDMessage("§wUnsupported game type");
      return;
    }

    args.isLengthEqualsElseExit(3, true);

    Collection<String> saveNames = Network.getNetworkUtils()
        .getPublicSaveNames(ServerType.GAME, ((DbNonTmpGame) game).getName());

    String saveName = args.getString(2);

    if (!saveNames.contains(saveName)) {
      sender.sendMessageServerNameNotExist(saveName);
      return;
    }

    DbGame finalGame = game;

    Network.runTaskAsync(() -> {
      sender.sendPluginTDMessage("§sLoading save §v" + saveName);
      ServerSetupResult result = Network.getServerManager().loadPublicSaveToServer(((DbNonTmpGame) finalGame).getName(),
          saveName, s -> s.applyServerOptions(finalGame.getServerOptions()));

      if (!result.isSuccessful()) {
        sender.sendPluginTDMessage("§wError while loading save (" +
                                   ((ServerSetupResult.Fail) result).getReason() + ")");
        return;
      }

      sender.sendPluginTDMessage("§sLoaded save §v" + saveName + "§s, you will be moved in a few moments");

      NonTmpGameServer server = (NonTmpGameServer) ((ServerSetupResult.Success) result).getServer();
      server.setMaxPlayers(((DbNonTmpGame) finalGame).getMaxPlayers());

      boolean isStart = server.start();
      if (!isStart) {
        sender.sendPluginTDMessage("§wError while starting server §v" + server.getName());
        return;
      }
      sender.sendPluginTDMessage("§sStarted server §v" + server.getName());
      if (sender.isPlayer(false)) {
        server.addWaitingUser(sender.getUser());
      }
    });
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
    Integer maxPlayers = game.getMaxPlayers();

    Availability gameMapAvailability = game.getMapAvailability();

    boolean mapsEnabled = args.getArgumentByString("maps") != null || gameMapAvailability.equals(Availability.REQUIRED);

    if (gameMapAvailability.equals(Availability.FORBIDDEN) && mapsEnabled) {
      sender.sendPluginTDMessage("§wGame §v" + gameName + "§w forbids maps");
      return;
    }

    boolean oldPvP = args.getArgumentByString("oldpvp") != null || args.getArgumentByString("1.8pvp") != null;

    Network.runTaskAsync(() -> {
      sender.sendPluginTDMessage("§sCreating server...");

      ServerSetupResult result = Network.getServerManager().createTmpServer(ServerType.GAME, s -> s.setTask(gameName)
          .options(o -> o.setWorldCopyType(mapsEnabled ? CopyType.COPY : CopyType.NONE))
          .applyServerOptions(game.getServerOptions()));

      if (!result.isSuccessful()) {
        sender.sendPluginTDMessage("§wError while creating a" + " game server! " +
                                   "Please contact an administrator ("
                                   + ((ServerSetupResult.Fail) result).getReason() + ")");
        return;
      }

      NonTmpGameServer server = (NonTmpGameServer) ((ServerSetupResult.Success) result).getServer();

      server.setTaskSynchronized(gameName);
      server.setMaxPlayers(maxPlayers);

      if (sender.isPlayer(false)) {
        sender.sendPluginMessage(Network.getTimeDownParser().parse2Component("§sStarted game §v" + gameName)
            .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                Network.getTimeDownParser().parse2Component(
                    "§sStarted game §v" + gameName + "\n" +
                    "§sGame server: §v" + server.getName() + "\n" +
                    "§sMax players: §v" + maxPlayers + "\n" +
                    "§sOld PvP: §v" + oldPvP + "\n"
                ))));
      } else {
        sender.sendPluginTDMessage("§sStarted game §v" + gameName);
        sender.sendPluginTDMessage("§sGame server: §v" + server.getName());
        sender.sendPluginTDMessage("§sMax players: §v" + maxPlayers);
        sender.sendPluginTDMessage("§sOld PvP: §v" + oldPvP);
      }

      boolean isStart = server.start();
      if (!isStart) {
        sender.sendPluginTDMessage("§wError while starting server §v" + server.getName());
        return;
      }
      sender.sendPluginTDMessage("§sStarted server §v" + server.getName());
    });
  }

  private void handleStartTmpGame(Sender sender, Arguments<Argument> args, DbTmpGame game) {
    String gameName = game.getName();
    Availability gameKitAvailability = game.getKitAvailability();
    Availability gameMapAvailability = game.getMapAvailability();
    Collection<Integer> gameTeamAmounts = game.getTeamAmounts();
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
      sender.sendPluginTDMessage("§sCreating server...");

      Tuple<ServerSetupResult, ServerSetupResult> servers = Network.getServerManager()
          .createTmpTwinServers(gameName, ServerType.LOUNGE, s -> {
              },
              ServerType.TEMP_GAME, s -> s.setTask(gameName)
                  .options(o -> o.setWorldCopyType(mapsEnabled ? CopyType.COPY : CopyType.NONE))
                  .applyServerOptions(game.getServerOptions()));

      ServerSetupResult loungeServerResult = servers.getA();
      ServerSetupResult gameServerResult = servers.getB();

      if (!loungeServerResult.isSuccessful()) {
        sender.sendPluginTDMessage("§wError while creating a lounge server! " +
                                   "Please contact an administrator " +
                                   "(" + ((ServerSetupResult.Fail) loungeServerResult).getReason() + ")");
        return;
      }

      if (!gameServerResult.isSuccessful()) {
        sender.sendPluginTDMessage("§wError while creating a " + gameName
                                   + " server! Please contact an administrator (" +
                                   ((ServerSetupResult.Fail) gameServerResult).getReason()
                                   + ")");
        return;
      }

      LoungeServer loungeServer = (LoungeServer) ((ServerSetupResult.Success) loungeServerResult).getServer();
      TmpGameServer tmpGameServer = (TmpGameServer) ((ServerSetupResult.Success) gameServerResult).getServer();

      loungeServer.setTaskSynchronized(gameName);
      loungeServer.setMaxPlayers(finalMaxServerPlayers);

      tmpGameServer.setTaskSynchronized(gameName);
      tmpGameServer.setMapsEnabled(mapsEnabled);
      tmpGameServer.setKitsEnabled(kitsEnabled);
      tmpGameServer.setMaxPlayers(finalPlayersPerTeam);
      tmpGameServer.setTeamAmount(finalTeamAmount);
      tmpGameServer.setMapsEnabled(mapsEnabled);
      tmpGameServer.setTeamMerging(teamMerging);
      tmpGameServer.setMaxPlayersPerTeam(finalPlayersPerTeam);
      tmpGameServer.setPvP(oldPvP);
      tmpGameServer.setTwinServer((DbLoungeServer) loungeServer.getDatabase());

      if (sender.isPlayer(false)) {
        sender.sendPluginMessage(Network.getTimeDownParser().parse2Component("§sStarted game §v" + gameName)
            .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                Network.getTimeDownParser().parse2Component(
                    "§sGame server: §v" + tmpGameServer.getName() + "\n" +
                    "§sLounge server: §v" + loungeServer.getName() + "\n" +
                    "§sMax players: §v" + finalMaxServerPlayers + "\n" +
                    "§sMaps: §v" + mapsEnabled + "\n" +
                    "§sKits: §v" + kitsEnabled + "\n" +
                    "§sTeam amount: §v" + finalTeamAmount + "\n" +
                    "§sTeam merging: §v" + teamMerging + "\n" +
                    "§sPlayer per Team: §v" + finalPlayersPerTeam + "\n" +
                    "§sOld PvP: §v" + oldPvP
                ))));
      } else {
        sender.sendPluginTDMessage("§sStarted game §v" + gameName);
        sender.sendPluginTDMessage("§sGame server: §v" + tmpGameServer.getName());
        sender.sendPluginTDMessage("§sLounge server: §v" + loungeServer.getName());
        sender.sendPluginTDMessage("§sMax players: §v" + finalMaxServerPlayers);
        sender.sendPluginTDMessage("§sMaps: §v" + mapsEnabled);
        sender.sendPluginTDMessage("§sKits: §v" + kitsEnabled);
        sender.sendPluginTDMessage("§sTeam amount: §v" + finalTeamAmount);
        sender.sendPluginTDMessage("§sTeam merging: §v" + teamMerging);
        sender.sendPluginTDMessage("§sPlayer per Team: §v" + finalPlayersPerTeam);
        sender.sendPluginTDMessage("§sOld PvP: §v" + oldPvP);
      }

      boolean isStart = loungeServer.start();
      if (!isStart) {
        sender.sendPluginTDMessage("§wError while starting server §v" + loungeServer.getName());
        return;
      }

      sender.sendPluginTDMessage("§sStarted server §v" + loungeServer.getName());
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
                    Network.getNetworkUtils().getPublicSaveNames(ServerType.GAME, args.getString(1)) : List.of())));
  }

  @Override
  public String getPermission() {
    return this.gamePerm.getPermission();
  }
}
