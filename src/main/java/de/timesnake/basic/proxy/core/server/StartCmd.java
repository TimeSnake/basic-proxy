/*
 * basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.*;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbNonTmpGame;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.extension.util.chat.Chat;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.ServerCreationResult;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.timesnake.library.basic.util.chat.ExTextColor.*;
import static net.kyori.adventure.text.Component.text;

public class StartCmd implements CommandListener<Sender, Argument> {

    private Code.Permission serverPerm;
    private Code.Permission ownGamePerm;
    private Code.Permission publicGamePerm;
    private Code.Permission gamePerm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.isLengthHigherEquals(2, true)) {
            switch (args.get(0).toLowerCase()) {
                case "server" -> {
                    if (!sender.hasPermission(this.serverPerm)) {
                        return;
                    }
                    this.handleStartServer(sender, args);
                }
                case "own_game" -> {
                    if (!sender.hasPermission(this.ownGamePerm)) {
                        return;
                    }
                    this.handleStartOwnGameServer(sender, args);
                }
                case "public_game" -> {
                    if (!sender.hasPermission(this.publicGamePerm)) {
                        return;
                    }
                    this.handleStartPublicGameServer(sender, args);
                }
                case "game" -> {
                    if (!sender.hasPermission(this.gamePerm)) {
                        return;
                    }
                    this.handleStartGame(sender, args);
                }
            }
        }
    }

    private void handleStartServer(Sender sender, Arguments<Argument> args) {
        String serverName = args.get(1).toLowerCase();
        Server server = Network.getServer(serverName);

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
        if (status.isRunning()) {
            sender.sendPluginMessage(text("Server ", WARNING)
                    .append(text(serverName, VALUE))
                    .append(text(" is already online!", WARNING)));
            return;
        }

        Integer maxPlayers = null;
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

    private void handleStartOwnGameServer(Sender sender, Arguments<Argument> args) {
        if (!sender.isPlayer(true)) {
            return;
        }

        User user = sender.getUser();

        String gameType = args.get(1).toLowerCase();

        // check game
        DbGame game = Database.getGames().getGame(gameType).toLocal();
        if (!game.exists()) {
            sender.sendMessageGameNotExist(gameType);
            return;
        }

        if (!(game instanceof DbNonTmpGame nonTmpGame)) {
            sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
            return;
        }

        if (!nonTmpGame.isOwnable()) {
            sender.sendPluginMessage(Component.text("Servers of this game can not have an owner", WARNING));
            return;
        }

        if (!args.isLengthEquals(3, true)) {
            return;
        }

        Collection<String> serverNames = Network.getNetworkUtils().getOwnerServerNames(user.getUniqueId(),
                Type.Server.GAME, ((DbNonTmpGame) game).getName());

        String serverName = args.getString(2);

        if (!serverNames.contains(serverName)) {
            sender.sendMessageServerNameNotExist(serverName);
            return;
        }

        int port = Network.nextEmptyPort();
        NetworkServer networkServer = new NetworkServer(user.getUniqueId().hashCode() + "_" + serverName, port,
                Type.Server.GAME, Network.getVelocitySecret()).setFolderName(serverName)
                .setTask(((DbNonTmpGame) game).getName()).setMaxPlayers(20).allowNether(true).allowEnd(true);

        sender.sendPluginMessage(Component.text("Loading server ", PERSONAL).append(Component.text(serverName, VALUE)));
        Tuple<ServerCreationResult, Optional<Server>> result = Network.loadPlayerServer(user.getUniqueId(), networkServer);

        if (!result.getA().isSuccessful()) {
            sender.sendPluginMessage(Component.text("Error while loading server (" +
                    ((ServerCreationResult.Fail) result.getA()).getReason(), WARNING));
            return;
        }

        sender.sendPluginMessage(Component.text("Loaded server ", PERSONAL)
                .append(Component.text(serverName, VALUE))
                .append(Component.text(", you will be moved in a few moments", PERSONAL)));

        NonTmpGameServer server = (NonTmpGameServer) result.getB().get();
        server.setMaxPlayers(((DbNonTmpGame) game).getMaxPlayers());
        server.setOwnerUuid(user.getUniqueId());

        Network.getBukkitCmdHandler().handleServerCmd(sender, server);
        server.addWaitingUser(user);
    }

    private void handleStartPublicGameServer(Sender sender, Arguments<Argument> args) {
        String gameType = args.get(1).toLowerCase();

        // check game
        DbGame game = Database.getGames().getGame(gameType).toLocal();
        if (!game.exists()) {
            sender.sendMessageGameNotExist(gameType);
            return;
        }

        if (!(game instanceof DbNonTmpGame nonTmpGame)) {
            sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
            return;
        }

        if (!args.isLengthEquals(3, true)) {
            return;
        }

        Collection<String> serverNames = Network.getNetworkUtils().getPublicPlayerServerNames(Type.Server.GAME, ((DbNonTmpGame) game).getName());

        String serverName = args.getString(2);

        if (!serverNames.contains(serverName)) {
            sender.sendMessageServerNameNotExist(serverName);
            return;
        }

        int port = Network.nextEmptyPort();
        NetworkServer networkServer = new NetworkServer(serverName, port, Type.Server.GAME,
                Network.getVelocitySecret()).setTask(((DbNonTmpGame) game).getName()).setMaxPlayers(20).allowNether(true).allowEnd(true);

        sender.sendPluginMessage(Component.text("Loading server ", PERSONAL).append(Component.text(serverName, VALUE)));
        Tuple<ServerCreationResult, Optional<Server>> result = Network.loadPublicPlayerServer(networkServer);

        if (!result.getA().isSuccessful()) {
            sender.sendPluginMessage(Component.text("Error while loading server (" +
                    ((ServerCreationResult.Fail) result.getA()).getReason(), WARNING));
            return;
        }

        sender.sendPluginMessage(Component.text("Loaded server ", PERSONAL)
                .append(Component.text(serverName, VALUE))
                .append(Component.text(", you will be moved in a few moments", PERSONAL)));

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
        DbGame game = Database.getGames().getGame(gameType).toLocal();
        if (!game.exists()) {
            sender.sendMessageGameNotExist(gameType);
            return;
        }

        if (game instanceof DbTmpGame) {
            this.handleStartTmpGame(sender, args, (DbTmpGame) game);
        } else {
            this.handleStartNonTmpGame(sender, args, (DbNonTmpGame) game);
        }
    }

    private void handleStartNonTmpGame(Sender sender, Arguments<Argument> args, DbNonTmpGame game) {
        if (game.isOwnable()) {
            sender.sendPluginMessage(text("Unsupported game type"));
            return;
        }


        String gameName = game.getName();
        Integer gameMaxPlayers = game.getMaxPlayers();

        Type.Availability gameKitAvailability = game.getKitAvailability();
        Type.Availability gameMapAvailability = game.getMapAvailability();

        // kits
        boolean kitsEnabled = args.getArgumentByString("kits") != null;

        if (gameKitAvailability.equals(Type.Availability.FORBIDDEN) && kitsEnabled) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text(" forbid kits", WARNING)));
            return;
        }

        if (gameKitAvailability.equals(Type.Availability.REQUIRED) && !kitsEnabled) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text(" require kits", WARNING)));
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
                    .append(text(" require maps", WARNING)));
            return;
        }

        boolean oldPvP = args.getArgumentByString("oldpvp") != null || args.getArgumentByString("1.8pvp") != null;
        boolean netherEnd = game.isNetherAndEndAllowed();

        sender.sendPluginMessage(text("Creating server...", PERSONAL));


        Network.runTaskAsync(() -> {
            int port = Network.nextEmptyPort();
            NetworkServer networkServer = new NetworkServer((port % 1000) + gameName + Network.TMP_SERVER_SUFFIX,
                    port, Type.Server.GAME, Network.getVelocitySecret()).setTask(gameName).allowEnd(netherEnd).allowNether(netherEnd);

            if (game.getInfo().getPlayerTrackingRange() != null) {
                networkServer.setPlayerTrackingRange(game.getInfo().getPlayerTrackingRange());
            }

            if (game.getInfo().getMaxHealth() != null) {
                networkServer.setMaxHealth(game.getInfo().getMaxHealth());
            }

            Tuple<ServerCreationResult, Optional<Server>> result = Network.createTmpServer(networkServer, mapsEnabled, false);
            if (!result.getA().isSuccessful()) {
                sender.sendPluginMessage(text("Error while creating a" + " game server! " +
                        "Please contact an administrator (" + ((ServerCreationResult.Fail) result.getA()).getReason() + ")", WARNING));
                return;
            }

            NonTmpGameServer server = (NonTmpGameServer) result.getB().get();

            server.setTaskSynchronized(gameName);
            server.setMaxPlayers(gameMaxPlayers);

            sender.sendPluginMessage(text("Started game ", PERSONAL).append(text(gameName, VALUE)));
            sender.sendPluginMessage(text("Game server: ", PERSONAL).append(text(server.getName(), VALUE)));
            sender.sendPluginMessage(text("Max players: ", PERSONAL).append(text("" + gameMaxPlayers, VALUE)));
            sender.sendPluginMessage(text("Old PvP: ", PERSONAL).append(text(oldPvP, VALUE)));

            Network.getBukkitCmdHandler().handleServerCmd(sender, server);
        });
    }

    private void handleStartTmpGame(Sender sender, Arguments<Argument> args, DbTmpGame game) {

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
                    .append(text(" forbid kits", WARNING)));
            return;
        }

        if (gameKitAvailability.equals(Type.Availability.REQUIRED) && !kitsEnabled) {
            sender.sendPluginMessage(text("Game ", WARNING)
                    .append(text(gameName, VALUE))
                    .append(text(" require kits", WARNING)));
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
                    .append(text(" require maps", WARNING)));
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
            sender.sendPluginMessage(text("Game ", WARNING).
                    append(text(gameName, VALUE))
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
            NetworkServer loungeNetworkServer = new NetworkServer((loungePort % 1000) +
                    Type.Server.LOUNGE.getDatabaseValue() + Network.TMP_SERVER_SUFFIX, loungePort, Type.Server.LOUNGE,
                    Network.getVelocitySecret());

            Tuple<ServerCreationResult, Optional<Server>> loungeResult = Network.createTmpServer(loungeNetworkServer, true, false);

            if (!loungeResult.getA().isSuccessful()) {
                sender.sendPluginMessage(text("Error while creating a" + " lounge server! " +
                        "Please contact an administrator (" + ((ServerCreationResult.Fail) loungeResult.getA()).getReason() + ")", WARNING));
                return;
            }

            int tempGamePort = Network.nextEmptyPort();

            NetworkServer gameNetworkServer = new NetworkServer((tempGamePort % 1000) + gameName + Network.TMP_SERVER_SUFFIX,
                    tempGamePort, Type.Server.TEMP_GAME, Network.getVelocitySecret()).setTask(gameName);

            if (game.getInfo().getPlayerTrackingRange() != null) {
                gameNetworkServer.setPlayerTrackingRange(game.getInfo().getPlayerTrackingRange());
            }

            if (game.getInfo().getMaxHealth() != null) {
                gameNetworkServer.setMaxHealth(game.getInfo().getMaxHealth());
            }

            Tuple<ServerCreationResult, Optional<Server>> tempServerResult = Network.createTmpServer(gameNetworkServer, mapsEnabled, false);
            if (!tempServerResult.getA().isSuccessful()) {
                sender.sendPluginMessage(text("Error while creating a" + " " + gameName + " server! Please contact an administrator (" +
                        ((ServerCreationResult.Fail) tempServerResult.getA()).getReason() + ")", WARNING));
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

            sender.sendPluginMessage(text("Started game ", PERSONAL).append(text(gameName, VALUE)));
            sender.sendPluginMessage(text("Game server: ", PERSONAL).append(text(tmpGameServer.getName(), VALUE)));
            sender.sendPluginMessage(text("Lounge server: ", PERSONAL).append(text(loungeServer.getName(), VALUE)));
            sender.sendPluginMessage(text("Max players: ", PERSONAL).append(text("" + finalMaxServerPlayers, VALUE)));
            sender.sendPluginMessage(text("Maps: ", PERSONAL).append(text(mapsEnabled, VALUE)));
            sender.sendPluginMessage(text("Kits: ", PERSONAL).append(text(kitsEnabled, VALUE)));
            sender.sendPluginMessage(text("Team amount: ", PERSONAL).append(text(finalTeamAmount, VALUE)));
            sender.sendPluginMessage(text("Team merging: ", PERSONAL).append(text(teamMerging, VALUE)));
            sender.sendPluginMessage(text("Max players per team: ", PERSONAL).append(text("" + finalPlayersPerTeam, VALUE)));
            sender.sendPluginMessage(text("Old PvP: ", PERSONAL).append(text(oldPvP, VALUE)));

            Network.getBukkitCmdHandler().handleServerCmd(sender, loungeServer);

        });
    }


    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        int length = args.getLength();
        if (length == 1 || length == 0) {
            return List.of("game", "server", "own_game", "public_game");
        }

        if (length == 2) {
            if (args.getString(0).equalsIgnoreCase("server")) {
                return Network.getCommandHandler().getServerNames();
            }

            if (args.getString(0).equalsIgnoreCase("game")) {
                return Network.getCommandHandler().getGameNames();
            }

            if (args.getString(0).equalsIgnoreCase("own_game")
                    || args.getString(0).equalsIgnoreCase("public_game")) {
                return Network.getCommandHandler().getGameNames();
            }
        }

        if (length == 3) {
            if (args.getString(0).equalsIgnoreCase("own_game")) {
                // TODO player server names
                return List.of();
            }

            if (args.getString(0).equalsIgnoreCase("public_game")) {
                if (Database.getGames().containsGame(args.getString(1).toLowerCase())) {
                    return Network.getNetworkUtils().getPublicPlayerServerNames(Type.Server.GAME, args.getString(1));
                }
                return List.of();
            }
        }
        return null;
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.serverPerm = plugin.createPermssionCode("prx", "network.start.server");
        this.gamePerm = plugin.createPermssionCode("prx", "network.start.game");
        this.ownGamePerm = plugin.createPermssionCode("prx", "network.start.own_game");
        this.publicGamePerm = plugin.createPermssionCode("prx", "network.start.public_game");
    }
}
