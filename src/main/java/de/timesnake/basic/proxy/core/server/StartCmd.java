package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.*;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
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

import static de.timesnake.library.basic.util.chat.ExTextColor.*;
import static net.kyori.adventure.text.Component.text;

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
                        sender.sendPluginMessage(text("Server ", WARNING)
                                .append(text(serverName, VALUE))
                                .append(text(" doesn't exist!", WARNING)));
                        return;
                    }
                    Status.Server status = server.getStatus();
                    if (status != null && (status.equals(Status.Server.LAUNCHING) || status.equals(Status.Server.LOADING))) {
                        sender.sendPluginMessage(text("Server ", WARNING)
                                .append(text(serverName, VALUE))
                                .append(text(" is already starting!", WARNING)));
                        return;
                    }
                    if (!(status == null || status.equals(Status.Server.OFFLINE))) {
                        sender.sendPluginMessage(text("Server ", WARNING)
                                .append(text(serverName, VALUE))
                                .append(text(" is already online!", WARNING)));
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
                        sender.sendPluginMessage(text("No default max-players value found", WARNING));
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
            sender.sendPluginMessage(text("Please use the /start server command to start a non temp " + "game server", WARNING));
            GameServer gameServer = null;
            for (Server server1 : Network.getServers()) {
                if (server1.getType().equals(Type.Server.GAME)
                        && (server1.getStatus() == null || server1.getStatus().equals(Status.Server.OFFLINE))
                        && ((GameServer) server1).getTask().equals(gameType)) {
                    gameServer = (GameServer) server1;
                }
            }

            if (gameServer == null) {
                sender.sendPluginMessage(text("All game servers are in use!", WARNING));
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
                sender.sendPluginMessage(text("No default max-players value found", WARNING));
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
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text("forbid kits", WARNING)));
            return;
        }

        if (gameKitAvailability.equals(Type.Availability.REQUIRED) && !kitsEnabled) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text("require kits", WARNING)));
            return;
        }

        // maps
        boolean mapsEnabled = args.getArgumentByString("maps") != null;

        if (gameMapAvailability.equals(Type.Availability.FORBIDDEN) && mapsEnabled) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text(" forbid maps", WARNING)));
            return;
        }

        if (gameMapAvailability.equals(Type.Availability.REQUIRED) && !mapsEnabled) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text("require maps", WARNING)));
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

        if (gameMergeTeams == null) {
            gameMergeTeams = Type.Availability.FORBIDDEN;
        }

        if (gameMergeTeams.equals(Type.Availability.FORBIDDEN) && teamMerging) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text(" forbid team merging", WARNING)));
            return;
        }

        if (gameMergeTeams.equals(Type.Availability.REQUIRED) && !teamMerging && teamAmount > 0) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text(" require team merging", WARNING)));
            return;
        }

        boolean oldPvP = args.getArgumentByString("oldpvp") != null || args.getArgumentByString("1.8pvp") != null;


        // search temp game and lounge server

        sender.sendPluginMessage(text("Creating server...", PERSONAL));

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
                sender.sendPluginMessage(text("Error while creation a" +
                        " lounge server! Please contact an administrator (" +
                        ((ServerCreationResult.Fail) loungeResult.getA()).getReason() + ")", WARNING));
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
                sender.sendPluginMessage(text("Error while creation a" +
                        " " + gameName + " server! Please contact an administrator (" +
                        ((ServerCreationResult.Fail) tempServerResult.getA()).getReason() + ")", WARNING));
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

            sender.sendPluginMessage(text("Started game ", PERSONAL).append(text(gameName, VALUE)));
            sender.sendPluginMessage(text("Game server: ", PERSONAL).append(text(tempGameServer.getName(), VALUE)));
            sender.sendPluginMessage(text("Lounge server: ", PERSONAL).append(text(loungeServer.getName(), VALUE)));
            sender.sendPluginMessage(text("Max players: ", PERSONAL).append(text(finalMaxServerPlayers, VALUE)));
            sender.sendPluginMessage(text("Maps: ", PERSONAL).append(text(mapsEnabled, VALUE)));
            sender.sendPluginMessage(text("Kits: ", PERSONAL).append(text(kitsEnabled, VALUE)));
            sender.sendPluginMessage(text("Team amount: ", PERSONAL).append(text(finalTeamAmount, VALUE)));
            sender.sendPluginMessage(text("Team merging: ", PERSONAL).append(text(teamMerging, VALUE)));
            sender.sendPluginMessage(text("Max players per team: ", PERSONAL).append(text(finalPlayersPerTeam, VALUE)));
            sender.sendPluginMessage(text("Old PvP: ", PERSONAL).append(text(oldPvP, VALUE)));

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
