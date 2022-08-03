package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.*;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.basic.util.chat.ChatColor;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.ServerCreationResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StartCmd implements CommandListener<Sender, Argument> {

    private static final String KITS = "kits";

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(2, true)) {
            Integer maxPlayers;
            switch (args.get(0).toLowerCase()) {
                case "server" -> {
                    String serverName = args.get(1).toLowerCase();
                    Server server = Network.getServer(serverName);
                    if (!sender.hasPermission("network.start.server", 46)) {
                        return;
                    }
                    if (server == null) {
                        sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + serverName +
                                ChatColor.WARNING + " doesn't exist!");
                        return;
                    }
                    Status.Server status = server.getStatus();
                    if (status != null && (status.equals(Status.Server.LAUNCHING) || status.equals(Status.Server.LOADING))) {
                        sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + serverName +
                                ChatColor.WARNING + " is already starting!");
                        return;
                    }
                    if (!(status == null || status.equals(Status.Server.OFFLINE))) {
                        sender.sendPluginMessage(ChatColor.WARNING + "Server " + ChatColor.VALUE + serverName +
                                ChatColor.WARNING + " is already online!");
                        return;
                    }
                    maxPlayers = null;
                    if (args.isLengthEquals(3, false) && args.get(2).isInt(false)) {
                        maxPlayers = args.get(2).toInt();
                    } else if (server instanceof GameServer && !(server instanceof BuildServer)) {
                        String task = ((TaskServer) server).getTask();
                        maxPlayers = Database.getGames().getGame(task).getInfo().getMaxPlayers();
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
                }
                case "game" -> {
                    if (!sender.hasPermission("network.start.game", 47)) {
                        return;
                    }
                    this.handleStartGame(sender, args);
                }
            }
        }
    }

    private void handleStartGame(Sender sender, Arguments<Argument> args) {
        String gameType = args.get(1).toLowerCase();

        // check game
        DbGame game = Database.getGames().getGame(gameType).toLocal();
        if (!game.exists()) {
            sender.sendMessageGameNotExist(gameType);
            return;
        }

        if (game instanceof DbTmpGame) {
            this.handleStartTempGame(sender, args, (DbTmpGame) game);
        } else {
            sender.sendPluginMessage(ChatColor.WARNING + "Please use the /start server command to start a non temp " + "game server");
            GameServer gameServer = null;
            for (Server server1 : Network.getServers()) {
                if (server1.getType().equals(Type.Server.GAME)
                        && (server1.getStatus() == null || server1.getStatus().equals(Status.Server.OFFLINE))
                        && ((GameServer) server1).getTask().equals(gameType)) {
                    gameServer = (GameServer) server1;
                }
            }

            if (gameServer == null) {
                sender.sendPluginMessage(ChatColor.WARNING + "All game servers are in use!");
                return;
            }

            gameServer.setTask(gameType);
            Integer maxPlayers = null;

            if (args.isLengthEquals(3, false)
                    && args.get(2).isInt(true)) {
                maxPlayers = args.get(2).toInt();
            } else if (Database.getGames().containsGame(gameType)) {
                maxPlayers = Database.getGames().getGame(gameType).getInfo().getMaxPlayers();
            }

            if (maxPlayers != null) {
                gameServer.setMaxPlayers(maxPlayers);
                Network.getBukkitCmdHandler().handleServerCmd(sender, gameServer);
            } else {
                sender.sendPluginMessage(ChatColor.WARNING + "No default max-players value found");
            }

        }
    }

    private void handleStartGame(Sender sender, Arguments<Argument> args, DbGame game) {

    }

    private void handleStartTempGame(Sender sender, Arguments<Argument> args, DbTmpGame game) {

        String gameName = game.getName();
        Type.Availability gameKitAvailability = game.getKitAvailability();
        Type.Availability gameMapAvailability = game.getMapAvailability();
        Collection<Integer> gameTeamAmounts = game.getTeamSizes();
        Integer gameMaxPlayers = game.getMaxPlayers();
        Integer gameMinPlayers = game.getMinPlayerNumber();
        Type.Availability gameMergeTeams = game.getTeamMergeAvailability();

        // kits
        boolean kitsEnabled = args.getArgumentByString("kits") != null;

        if (gameKitAvailability.equals(Type.Availability.FORBIDDEN) && kitsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING +
                    " forbid kits");
            return;
        }

        if (gameKitAvailability.equals(Type.Availability.REQUIRED) && !kitsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING +
                    " require kits");
            return;
        }

        // maps
        boolean mapsEnabled = args.getArgumentByString("maps") != null;

        if (gameMapAvailability.equals(Type.Availability.FORBIDDEN) && mapsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING +
                    " forbid maps");
            return;
        }

        if (gameMapAvailability.equals(Type.Availability.REQUIRED) && !mapsEnabled) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING +
                    " require maps");
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
                sender.sendPluginMessage(ChatColor.WARNING + "Invalid max player amount");
                return;
            }

            if (maxServerPlayers > gameMaxPlayers) {
                sender.sendPluginMessage(ChatColor.WARNING + "Too large max players amount for game " +
                        ChatColor.VALUE + gameName + ChatColor.WARNING + ", max is " + ChatColor.VALUE + gameMaxPlayers);
                return;
            }

            if (maxServerPlayers < gameMinPlayers) {
                sender.sendPluginMessage(ChatColor.WARNING + "Too small max players amount for game " +
                        ChatColor.VALUE + gameName + ChatColor.WARNING + ", min is " + ChatColor.VALUE + gameMinPlayers);
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
                sender.sendPluginMessage(ChatColor.WARNING + "Invalid max players per team amount");
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
                    sender.sendPluginMessage(ChatColor.WARNING + "Invalid team amount");
                    return;
                }

                if (!gameTeamAmounts.contains(teamAmount)) {
                    sender.sendPluginMessage(ChatColor.WARNING + "Invalid team amount");
                    sender.sendPluginMessage(ChatColor.PERSONAL + "Available team amounts: " + ChatColor.VALUE +
                            Chat.listToString(gameTeamAmounts));
                    return;
                }
            }
        }


        boolean teamMerging = argMaxPerTeam != null && args.getArgumentByString("merge") != null;

        if (gameMergeTeams == null) {
            gameMergeTeams = Type.Availability.FORBIDDEN;
        }

        if (gameMergeTeams.equals(Type.Availability.FORBIDDEN) && teamMerging) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING +
                    " forbid team merging");
            return;
        }

        if (gameMergeTeams.equals(Type.Availability.REQUIRED) && !teamMerging && teamAmount > 0) {
            sender.sendPluginMessage(ChatColor.WARNING + "Game " + ChatColor.VALUE + gameName + ChatColor.WARNING +
                    " require team merging");
            return;
        }

        boolean oldPvP = args.getArgumentByString("oldpvp") != null || args.getArgumentByString("1.8pvp") != null;


        // search temp game and lounge server

        sender.sendPluginMessage(ChatColor.PERSONAL + "Creating server...");

        Integer finalMaxServerPlayers = maxServerPlayers;
        Integer finalTeamAmount = teamAmount;
        Integer finalPlayersPerTeam = playersPerTeam;

        Network.runTaskAsync(() -> {
            int loungePort = Network.nextEmptyPort();
            NetworkServer loungeNetworkServer =
                    new NetworkServer((loungePort % 1000) + Type.Server.LOUNGE.getDatabaseValue() + Network.TMP_SERVER_SUFFIX,
                            loungePort, Type.Server.LOUNGE, Network.getVelocitySecret());

            Tuple<ServerCreationResult, Optional<Server>> loungeResult = Network.newServer(loungeNetworkServer, true);
            if (!loungeResult.getA().isSuccessful()) {
                sender.sendMessage(Chat.getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING + "Error while creation a" +
                        " lounge server! Please contact an administrator (" +
                        ((ServerCreationResult.Fail) loungeResult.getA()).getReason() + ")");
                return;
            }

            int tempGamePort = Network.nextEmptyPort();

            NetworkServer gameNetworkServer =
                    new NetworkServer((tempGamePort % 1000) + gameName + Network.TMP_SERVER_SUFFIX,
                            tempGamePort, Type.Server.TEMP_GAME, Network.getVelocitySecret()).setTask(gameName);

            if (game.getInfo().getPlayerTrackingRange() != null) {
                gameNetworkServer.setPlayerTrackingRange(game.getInfo().getPlayerTrackingRange());
            }

            Tuple<ServerCreationResult, Optional<Server>> tempServerResult = Network.newServer(gameNetworkServer,
                    mapsEnabled);
            if (!tempServerResult.getA().isSuccessful()) {
                sender.sendMessage(Chat.getSenderPlugin(Plugin.NETWORK) + ChatColor.WARNING + "Error while creation a" +
                        " " + gameName + " server! Please contact an administrator (" +
                        ((ServerCreationResult.Fail) tempServerResult.getA()).getReason() + ")");
                return;
            }

            LoungeServer loungeServer = (LoungeServer) loungeResult.getB().get();
            TempGameServer tempGameServer = ((TempGameServer) tempServerResult.getB().get());

            loungeServer.setTaskSynchronized(gameName);
            loungeServer.setMaxPlayers(finalMaxServerPlayers);

            tempGameServer.setTaskSynchronized(gameName);
            tempGameServer.setMapsEnabled(mapsEnabled);
            tempGameServer.setKitsEnabled(kitsEnabled);
            tempGameServer.setMaxPlayers(finalMaxServerPlayers);
            tempGameServer.setTeamAmount(finalTeamAmount);
            tempGameServer.setMapsEnabled(mapsEnabled);
            tempGameServer.setTeamMerging(teamMerging);
            tempGameServer.setMaxPlayersPerTeam(finalPlayersPerTeam);
            tempGameServer.setPvP(oldPvP);
            tempGameServer.setTwinServer((DbLoungeServer) loungeServer.getDatabase());

            sender.sendPluginMessage(ChatColor.PERSONAL + "Started game " + ChatColor.VALUE + gameName);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Game server: " + ChatColor.VALUE + tempGameServer.getName());
            sender.sendPluginMessage(ChatColor.PERSONAL + "Lounge server: " + ChatColor.VALUE + loungeServer.getName());
            sender.sendPluginMessage(ChatColor.PERSONAL + "Max players: " + ChatColor.VALUE + finalMaxServerPlayers);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Maps: " + ChatColor.VALUE + mapsEnabled);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Kits: " + ChatColor.VALUE + kitsEnabled);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Team amount: " + ChatColor.VALUE + finalTeamAmount);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Team merging: " + ChatColor.VALUE + teamMerging);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Max players per team: " + ChatColor.VALUE + finalPlayersPerTeam);
            sender.sendPluginMessage(ChatColor.PERSONAL + "Old PvP: " + ChatColor.VALUE + oldPvP);

            Network.getBukkitCmdHandler().handleServerCmd(sender, loungeServer);

        });
    }


    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 1 || length == 0) {
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
