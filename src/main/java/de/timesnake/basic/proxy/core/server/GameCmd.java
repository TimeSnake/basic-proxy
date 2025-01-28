/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.IncCommandListener;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.LoungeServer;
import de.timesnake.basic.proxy.util.server.NonTmpGameServer;
import de.timesnake.basic.proxy.util.server.ServerSetupResult;
import de.timesnake.basic.proxy.util.server.TmpGameServer;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbNonTmpGame;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Availability;
import de.timesnake.library.basic.util.ServerType;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.inchat.IncCommandContext;
import de.timesnake.library.commands.inchat.IncCommandOption;
import de.timesnake.library.commands.simple.Arguments;
import de.timesnake.library.network.NetworkServer.CopyType;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.*;
import java.util.stream.IntStream;

public class GameCmd extends IncCommandListener {

  private final Code perm = Plugin.GAME.createPermssionCode("network.start.game");

  @Override
  public IncCommandContext onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);

    List<String> games = new ArrayList<>(Database.getGames().getGamesName());
    games.sort(String::compareTo);

    this.sendSelectionTo(sender, this.createSelection(GAME).addValues(games));

    return new IncCommandContext();
  }

  @Override
  public List<String> getTabCompletion(PluginCommand cmd, Arguments<Argument> args) {
    return List.of();
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }

  @Override
  public <V> boolean onUpdate(Sender sender, IncCommandContext context, IncCommandOption<V> option, V value) {
    if (GAME.equals(option)) {
      return this.checkMaxPlayers(sender, context);
    } else if (MAX_PLAYERS.equals(option)) {
      return this.checkMaps(sender, context);
    } else if (MAPS.equals(option)) {
      return this.checkKits(sender, context);
    } else if (KITS.equals(option)) {
      return this.checkTeamsSizes(sender, context);
    } else if (PLAYERS_PER_TEAM.equals(option)) {
      return this.checkOldPvP(sender, context);
    } else if (OLD_PVP.equals(option)) {
      return this.startGame(sender, context);
    }

    return false;
  }

  private boolean checkMaxPlayers(Sender sender, IncCommandContext context) {
    if (context.getOption(GAME) instanceof DbTmpGame game) {
      this.sendSelectionTo(sender, this.createSelection(MAX_PLAYERS)
          .addValues(IntStream.range(game.getMinPlayerNumber(), game.getMaxPlayers() + 1)
              .mapToObj(String::valueOf).toList()));
    } else if (context.getOption(GAME) instanceof DbNonTmpGame game) {
      this.sendSelectionTo(sender, this.createSelection(MAX_PLAYERS)
          .addValues(IntStream.range(2, game.getMaxPlayers() + 1)
              .mapToObj(String::valueOf).toList()));
    }
    return false;
  }

  private boolean checkMaps(Sender sender, IncCommandContext context) {
    Availability maps = context.getOption(GAME).getInfo().getMapAvailability();
    if (maps == Availability.ALLOWED) {
      this.sendSelectionTo(sender, this.createSelection(MAPS).addValues("yes", "no"));
    } else if (maps == Availability.REQUIRED) {
      context.addOption(MAPS, true);
      return this.checkKits(sender, context);
    } else if (maps == Availability.FORBIDDEN) {
      context.addOption(MAPS, false);
      return this.checkKits(sender, context);
    }
    return false;
  }

  private boolean checkKits(Sender sender, IncCommandContext context) {
    Availability kits = context.getOption(GAME).getInfo().getKitAvailability();
    if (kits == Availability.ALLOWED) {
      this.sendSelectionTo(sender, this.createSelection(KITS).addValues("yes", "no"));
    } else if (kits == Availability.REQUIRED) {
      context.addOption(KITS, true);
      return this.checkTeamsSizes(sender, context);
    } else if (kits == Availability.FORBIDDEN) {
      context.addOption(KITS, false);
      return this.checkTeamsSizes(sender, context);
    }
    return false;
  }

  private boolean checkTeamsSizes(Sender sender, IncCommandContext context) {
    if (context.getOption(GAME) instanceof DbTmpGame game) {
      LinkedList<String> sizes = new LinkedList<>();

      List<Integer> teams = new ArrayList<>(game.getTeamSizes());
      teams.sort(Integer::compareTo);
      teams.sort(Comparator.reverseOrder());

      if (teams.isEmpty()) {
        context.addOption(PLAYERS_PER_TEAM, null);
        context.addOption(TEAM_MERGE, false);
        return this.checkOldPvP(sender, context);
      } else if (teams.size() == 1) {
        context.addOption(TEAM_AMOUNT, teams.getFirst());
        context.addOption(PLAYERS_PER_TEAM, null);
        context.addOption(TEAM_MERGE, false);
        return this.checkOldPvP(sender, context);
      }

      int max = context.getOption(MAX_PLAYERS);

      if (teams.contains(0)) {
        sizes.add("solo");
        teams.remove(((Integer) 0));
      }

      for (Integer teamNumber : teams) {
        int teamSize = max / teamNumber;

        if (teamSize >= 2 && !sizes.contains(String.valueOf(teamSize))) {
          sizes.add(String.valueOf(teamSize));
        }
      }

      context.addOption(TEAM_MERGE, true);

      this.sendSelectionTo(sender, this.createSelection(PLAYERS_PER_TEAM).addValues(sizes));
    } else if (context.getOption(GAME) instanceof DbNonTmpGame) {
      return this.checkOldPvP(sender, context);
    }
    return false;
  }

  private boolean checkOldPvP(Sender sender, IncCommandContext context) {
    Availability oldPvP = context.getOption(GAME).getInfo().getOldPvPAvailability();
    if (oldPvP == Availability.ALLOWED) {
      this.sendSelectionTo(sender, this.createSelection(OLD_PVP).addValues("yes", "no"));
      return false;
    } else if (oldPvP == Availability.REQUIRED) {
      context.addOption(OLD_PVP, true);
      return this.startGame(sender, context);
    } else if (oldPvP == Availability.FORBIDDEN) {
      context.addOption(OLD_PVP, false);
      return this.startGame(sender, context);
    }
    return false;
  }

  private boolean startGame(Sender sender, IncCommandContext context) {

    if (context.getOption(GAME) instanceof DbTmpGame game) {
      String gameName = game.getInfo().getName();
      Boolean mapsEnabled = context.getOption(MAPS);
      Boolean kitsEnabled = context.getOption(KITS);
      Integer maxServerPlayers = context.getOption(MAX_PLAYERS);
      Integer teamSize = context.getOption(PLAYERS_PER_TEAM);
      Integer teamAmount = teamSize != null ? (int) Math.ceil(maxServerPlayers / ((double) teamSize)) :
          context.getOption(TEAM_AMOUNT);
      Boolean teamMerging = context.getOption(TEAM_MERGE);
      Boolean oldPvP = context.getOption(OLD_PVP);

      Network.runTaskAsync(() -> {
        sender.sendPluginTDMessage("§sCreating server...");

        Tuple<ServerSetupResult, ServerSetupResult> servers = Network.getServerManager()
            .createTmpTwinServers(ServerType.LOUNGE, s -> {
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
        loungeServer.setMaxPlayers(maxServerPlayers);

        tmpGameServer.setTaskSynchronized(gameName);
        tmpGameServer.setMapsEnabled(mapsEnabled);
        tmpGameServer.setKitsEnabled(kitsEnabled);
        tmpGameServer.setMaxPlayers(maxServerPlayers);
        tmpGameServer.setTeamAmount(teamAmount);
        tmpGameServer.setMapsEnabled(mapsEnabled);
        tmpGameServer.setTeamMerging(teamMerging);
        tmpGameServer.setMaxPlayersPerTeam(teamSize);
        tmpGameServer.setPvP(oldPvP);
        tmpGameServer.setTwinServer((DbLoungeServer) loungeServer.getDatabase());

        if (sender.isPlayer(false)) {
          sender.sendPluginMessage(Network.getTimeDownParser().parse2Component("§sStarted game §v" + gameName)
              .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                  Network.getTimeDownParser().parse2Component(
                      "§sGame server: §v" + tmpGameServer.getName() + "\n" +
                      "§sLounge server: §v" + loungeServer.getName() + "\n" +
                      "§sMax players: §v" + maxServerPlayers + "\n" +
                      "§sMaps: §v" + mapsEnabled + "\n" +
                      "§sKits: §v" + kitsEnabled + "\n" +
                      "§sTeam amount: §v" + teamAmount + "\n" +
                      "§sTeam merging: §v" + teamMerging + "\n" +
                      "§sPlayer per Team: §v" + teamSize + "\n" +
                      "§sOld PvP: §v" + oldPvP
                  ))));
        } else {
          sender.sendPluginTDMessage("§sStarted game §v" + gameName);
          sender.sendPluginTDMessage("§sGame server: §v" + tmpGameServer.getName());
          sender.sendPluginTDMessage("§sLounge server: §v" + loungeServer.getName());
          sender.sendPluginTDMessage("§sMax players: §v" + maxServerPlayers);
          sender.sendPluginTDMessage("§sMaps: §v" + mapsEnabled);
          sender.sendPluginTDMessage("§sKits: §v" + kitsEnabled);
          sender.sendPluginTDMessage("§sTeam amount: §v" + teamAmount);
          sender.sendPluginTDMessage("§sTeam merging: §v" + teamMerging);
          sender.sendPluginTDMessage("§sPlayer per Team: §v" + teamSize);
          sender.sendPluginTDMessage("§sOld PvP: §v" + oldPvP);
        }

        boolean isStart = loungeServer.start();
        if (!isStart) {
          sender.sendPluginTDMessage("§wError while starting server §v" + loungeServer.getName());
          return;
        }

        sender.sendPluginTDMessage("§sStarted server §v" + loungeServer.getName());
      });
    } else if (context.getOption(GAME) instanceof DbNonTmpGame game) {
      String gameName = game.getName();
      Boolean mapsEnabled = context.getOption(MAPS);
      Integer maxPlayers = context.getOption(MAX_PLAYERS);
      Boolean oldPvP = context.getOption(OLD_PVP);

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

    return true;
  }

  @Override
  public Collection<IncCommandOption<?>> getOptions() {
    return OPTIONS;
  }

  @Override
  public String getCommand() {
    return "game";
  }

  private static final IncCommandOption<DbGame> GAME = new IncCommandOption<>("game", "Game") {
    @Override
    public DbGame parseValue(String key) {
      return Database.getGames().getGame(key);
    }
  };
  private static final IncCommandOption<Integer> MAX_PLAYERS = new IncCommandOption.Int("max_players", "Max Players");
  private static final IncCommandOption<Boolean> MAPS = new IncCommandOption.Bool("maps", "Maps");
  private static final IncCommandOption<Boolean> KITS = new IncCommandOption.Bool("kits", "Kits");
  private static final IncCommandOption<Integer> PLAYERS_PER_TEAM = new IncCommandOption<>("players_per_team",
      "Players per Team") {
    @Override
    public Integer parseValue(String key) {
      if (key.equalsIgnoreCase("solo")) {
        return null;
      }
      return Integer.valueOf(key);
    }
  };
  private static final IncCommandOption<Integer> TEAM_AMOUNT = new IncCommandOption.Int("team_amount", "Team Amount");
  private static final IncCommandOption<Boolean> TEAM_MERGE = new IncCommandOption.Bool("team_merge", "Team Merge");
  private static final IncCommandOption<Boolean> OLD_PVP = new IncCommandOption.Bool("pvp", "Old PvP");

  private static final List<IncCommandOption<?>> OPTIONS = List.of(GAME, MAX_PLAYERS, MAPS, KITS,
      PLAYERS_PER_TEAM, TEAM_MERGE, OLD_PVP);
}
