package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.*;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.object.Status;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.cmd.Arguments;
import de.timesnake.library.basic.util.cmd.CommandListener;
import de.timesnake.library.basic.util.cmd.ExCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StartCmd implements CommandListener<Sender, Argument> {

    private static final String KITS = "kits";

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(2, true)) {
            Integer maxPlayers;
            switch (args.get(0).toLowerCase()) {
                case "server":

                    String serverName = args.get(1).toLowerCase();
                    Server server = Network.getServer(serverName);

                    if (!sender.hasPermission("network.start.server", 46)) {
                        return;
                    }

                    if (server == null) {
                        sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + serverName + ChatColor.WARNING + " doesn't exist!");
                        return;
                    }

                    Status.Server status = server.getStatus();

                    if (!(status == null || status.equals(Status.Server.OFFLINE))) {
                        sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + serverName + ChatColor.WARNING + " is already online!");
                        return;
                    }

                    if (status != null && status.equals(Status.Server.STARTING)) {
                        sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + serverName + ChatColor.WARNING + " is already starting!");
                        return;
                    }

                    maxPlayers = null;

                    if (args.isLengthEquals(3, false) && args.get(2).isInt(false)) {
                        maxPlayers = args.get(2).toInt();
                    } else if (server instanceof GameServer && !(server instanceof BuildServer)) {
                        String task = ((TaskServer) server).getTask();
                        maxPlayers = Database.getGames().getGame(task).getMaxPlayers();
                    } else if (server.getType().equals(Type.Server.LOBBY)) {
                        maxPlayers = Network.getMaxPlayersLobby();
                    } else if (server.getType().equals(Type.Server.BUILD)) {
                        maxPlayers = Network.getMaxPlayersBuild();
                    }

                    if (maxPlayers != null) {
                        server.setMaxPlayers(maxPlayers);
                        Network.getBukkitCmdHandler().handleServerCmd(sender, server);
                    } else {
                        sender.sendPluginMessage(ChatColor.WARNING + "No default max-players value found");
                    }
                    break;
                case "game":

                    if (!sender.hasPermission("network.start.game", 47)) {
                        return;
                    }

                    this.handleStartGame(sender, args);
                    break;
            }
        }
    }

    private void handleStartGame(Sender sender, Arguments<Argument> args) {
        String gameType = args.get(1).toLowerCase();

        // check game
        DbGame game = Database.getGames().getGame(gameType);
        if (!game.exists()) {
            sender.sendMessageGameNotExist(gameType);
            return;
        }

        if (game.isTemporary()) {
            this.handleStartTempGame(sender, args, game);
        } else {
            sender.sendPluginMessage(ChatColor.WARNING + "Please use the /start server command to start a non temp " + "game server");
			/*GameServer gameServer = null;
			for (Server server1 : Network.getServers()) {
				if (server1.getType().equals(Type.Server.GAME)
						&& (server1.getStatus() == null || server1.getStatus().equals(Status.Server.OFFLINE))
						&& ((GameServer) server1).getTask().equals(gameType)) {
					gameServer = (GameServer) server1;
				}
			}

			if (gameServer == null) {
				sender.sendPluginMessage(ChatColor.WARNING +"All game servers are in use!");
				return;
			}

			gameServer.setTask(gameType);
			gameServer.setKitsEnabled(areKitsEnabled);
			maxPlayers = null;

			if (args.isLengthEquals(3, false)
					&& args.get(2).isInt(true)) {
				maxPlayers = args.get(2).toInt();
			} else if (Database.getGames().containsGame(gameType)) {
				maxPlayers = Database.getGames().getGame(gameType).getMaxPlayers();
			}

			if (maxPlayers != null) {
				gameServer.setMaxPlayers(maxPlayers);
				Network.getBukkitCmdHandler().handleServerCmd(sender, gameServer);
			} else {
				sender.sendPluginMessage(ChatColor.WARNING+"No default max-players value found");
			}

			 */
        }
    }

    private void handleStartTempGame(Sender sender, Arguments<Argument> args, DbGame game) {

        String gameName = game.getName();
        Type.Availability gameKitAvailability = game.getKitAvailability();
        Type.Availability gameMapAvailability = game.getMapAvailability();
        Collection<Integer> gameTeamAmounts = game.getTeamAmounts();
        Integer gameMaxPlayers = game.getMaxPlayers();
        Integer gameMinPlayers = game.getMinPlayers();
        Type.Availability gameMergeTeams = game.getTeamMergeAvailability();

        // kits
        boolean kitsEnabled = args.getArgumentByString("kits") != null;

        if (gameKitAvailability.equals(Type.Availability.FORBIDDEN) && kitsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING + " forbid kits");
            return;
        }

        if (gameKitAvailability.equals(Type.Availability.REQUIRED) && !kitsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING + " require kits");
            return;
        }

        // maps
        boolean mapsEnabled = args.getArgumentByString("maps") != null;

        if (gameMapAvailability.equals(Type.Availability.FORBIDDEN) && mapsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING + " forbid maps");
            return;
        }

        if (gameMapAvailability.equals(Type.Availability.REQUIRED) && !mapsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING + " require maps");
            return;
        }

        // team amount
        int teamAmount = Collections.max(gameTeamAmounts);
        Argument teamsArg = args.getArgumentByStringPart("teams=");
        if (teamsArg != null) {
            String teams = teamsArg.getString().replace("teams=", "");
            try {
                teamAmount = Integer.parseInt(teams);
            } catch (NumberFormatException e) {
                sender.sendPluginMessage(ChatColor.WARNING + "Invalid team amount");
                return;
            }

            if (!gameTeamAmounts.contains(teamAmount)) {
                sender.sendPluginMessage(ChatColor.WARNING + "Invalid team amount");
                sender.sendPluginMessage(ChatColor.PERSONAL + "Available team amounts: " + ChatColor.VALUE + Arrays.toString(gameTeamAmounts.toArray()).replace("[", "").replace("]", ""));
                return;
            }
        }

        // check player amount
        Integer maxServerPlayers = gameMaxPlayers;
        Argument argMaxPlayers = args.getArgumentByStringPart("max=");
        if (argMaxPlayers != null) {
            String stringMax = argMaxPlayers.getString().replace("max=", "");
            try {
                maxServerPlayers = Integer.parseInt(stringMax);
            } catch (NumberFormatException e) {
                sender.sendPluginMessage(ChatColor.WARNING + "Invalid max player amount");
                return;
            }

            if (maxServerPlayers > gameMaxPlayers) {
                sender.sendPluginMessage(ChatColor.WARNING + "Too large max players amount for game " + ChatColor.VALUE + gameName + ChatColor.WARNING + ", max is " + ChatColor.VALUE + gameMaxPlayers);
                return;
            }

            if (maxServerPlayers < gameMinPlayers) {
                sender.sendPluginMessage(ChatColor.WARNING + "Too small max players amount for game " + ChatColor.VALUE + gameName + ChatColor.WARNING + ", min is " + ChatColor.VALUE + gameMinPlayers);
                return;
            }
        }

        // check players per team
        Integer playersPerTeam = null;
        Argument argMaxPerTeam = args.getArgumentByStringPart("mppt=");
        if (argMaxPerTeam != null) {
            String stringMaxPerTeam = argMaxPerTeam.getString().replace("mppt=", "");
            try {
                playersPerTeam = Integer.parseInt(stringMaxPerTeam);
            } catch (NumberFormatException e) {
                sender.sendPluginMessage(ChatColor.WARNING + "Invalid max players per team amount");
                return;
            }

            int minPerTeam = (int) Math.ceil(maxServerPlayers / ((double) teamAmount));
            if (playersPerTeam > minPerTeam) {
                sender.sendPluginMessage(ChatColor.WARNING + "Too small players-per-team amount for game " + ChatColor.VALUE + gameName + ChatColor.WARNING + ", min is " + ChatColor.VALUE + minPerTeam);
                return;
            }
        }

        boolean teamMerging = argMaxPerTeam != null && args.getArgumentByString("merge") != null;

        if (gameMergeTeams == null) {
            gameMergeTeams = Type.Availability.FORBIDDEN;
        }

        if (gameMergeTeams.equals(Type.Availability.FORBIDDEN) && teamMerging) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING + " forbid team merging");
            return;
        }

        if (gameMergeTeams.equals(Type.Availability.REQUIRED) && !teamMerging) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING + " require team merging");
            return;
        }

        boolean oldPvP = args.getArgumentByString("oldpvp") != null || args.getArgumentByString("1.8pvp") != null;


        // search temp game and lounge server
        LoungeServer loungeServer = null;
        TempGameServer gameServer = null;

        for (Server s : Network.getServers()) {
            String name = s.getName();
            Status.Server status = s.getStatus();
            if (s.getType().equals(Type.Server.LOUNGE)) {
                if (status == null || status.equals(Status.Server.OFFLINE)) {
                    loungeServer = ((LoungeServer) s);
                }
            } else if (s.getType().equals(Type.Server.TEMP_GAME)) {
                if ((status == null || status.equals(Status.Server.OFFLINE)) && ((TempGameServer) s).getTask() != null && (((TempGameServer) s).getTask().equalsIgnoreCase(gameName))) {
                    if (((TempGameServer) s).getTwinServer() == null || ((TempGameServer) s).getTwinServer().exists()) {
                        gameServer = ((TempGameServer) s);
                    }
                }
            }
        }

        if (gameServer == null) {
            sender.sendMessage(Network.getChat().getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING + "All game servers are in use!");
            return;
        }

        if (loungeServer == null) {
            sender.sendMessage(Network.getChat().getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING + "All lounge servers are in use!");
            return;
        }

        // update server database

        loungeServer.setTaskSynchronized(gameName);
        loungeServer.setMaxPlayers(maxServerPlayers);

        gameServer.setTaskSynchronized(gameName);
        gameServer.setMapsEnabled(mapsEnabled);
        gameServer.setKitsEnabled(kitsEnabled);
        gameServer.setMaxPlayers(maxServerPlayers);
        gameServer.setTeamAmount(teamAmount);
        gameServer.setMapsEnabled(mapsEnabled);
        gameServer.setTeamMerging(teamMerging);
        gameServer.setMaxPlayersPerTeam(playersPerTeam);
        gameServer.setPvP(oldPvP);
        gameServer.setTwinServer((DbLoungeServer) loungeServer.getDatabase());

        sender.sendPluginMessage(ChatColor.PERSONAL + "Started game " + ChatColor.VALUE + gameName);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Game server: " + ChatColor.VALUE + gameServer.getName());
        sender.sendPluginMessage(ChatColor.PERSONAL + "Lounge server: " + ChatColor.VALUE + loungeServer.getName());
        sender.sendPluginMessage(ChatColor.PERSONAL + "Max players: " + ChatColor.VALUE + maxServerPlayers);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Maps: " + ChatColor.VALUE + mapsEnabled);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Kits: " + ChatColor.VALUE + kitsEnabled);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Team amount: " + ChatColor.VALUE + teamAmount);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Team merging: " + ChatColor.VALUE + teamMerging);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Max players per team: " + ChatColor.VALUE + playersPerTeam);
        sender.sendPluginMessage(ChatColor.PERSONAL + "Old PvP: " + ChatColor.VALUE + oldPvP);

        Network.getBukkitCmdHandler().handleServerCmd(sender, loungeServer);
    }


    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 1) {
            return List.of("game", "server");
        }

        if (length == 2) {
            if (args.getString(0).equalsIgnoreCase("server")) {
                return Network.getCommandHandler().getServerNames();
            }

            if (args.getString(0).equalsIgnoreCase("game")) {
                return Network.getCommandHandler().getGameNames();
            }
        }
        return null;
    }
}
