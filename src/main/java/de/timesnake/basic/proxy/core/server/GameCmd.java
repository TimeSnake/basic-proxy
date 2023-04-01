/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.server;

import static de.timesnake.library.chat.ExTextColor.PERSONAL;
import static de.timesnake.library.chat.ExTextColor.WARNING;
import static net.kyori.adventure.text.Component.text;

import de.timesnake.basic.proxy.core.server.GameCmd.Context;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Plugin;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.server.LoungeServer;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.basic.proxy.util.server.TmpGameServer;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbNonTmpGame;
import de.timesnake.database.util.game.DbTmpGame;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.object.Type.Availability;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.cmd.ExCommand;
import de.timesnake.library.extension.util.cmd.IncCommandContext;
import de.timesnake.library.extension.util.cmd.IncCommandListener;
import de.timesnake.library.extension.util.cmd.IncCommandOption;
import de.timesnake.library.network.NetworkServer;
import de.timesnake.library.network.ServerCreationResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class GameCmd extends IncCommandListener<Sender, Argument, Context> {

    private final Code permCode = Plugin.GAME.createPermssionCode("network.start.game");

    @Override
    public Context onCommand(Sender sender, ExCommand<Sender, Argument> cmd) {

        sender.hasPermissionElseExit(this.permCode);

        List<String> games = new ArrayList<>(Database.getGames().getGamesName());
        games.sort(String::compareTo);

        this.sendSelectionTo(sender, this.createSelection(GAME)
                .addValues(games));

        return new Context();
    }

    @Override
    public <V> boolean onUpdate(Sender sender, Context context, IncCommandOption<V> option,
            V value) {
        System.out.println(option.getName());

        if (GAME.equals(option)) {
            this.checkMaxPlayers(sender, context);
        } else if (MAX_PLAYERS.equals(option)) {
            this.checkMaps(sender, context);
        } else if (MAPS.equals(option)) {
            this.checkKits(sender, context);
        } else if (KITS.equals(option)) {
            this.checkTeamsSizes(sender, context);
        } else if (TEAM_SIZE.equals(option)) {
            this.checkOldPvP(sender, context);
        } else if (OLD_PVP.equals(option)) {
            this.startGame(sender, context);
            return true;
        }

        return false;
    }

    private void checkMaxPlayers(Sender sender, Context context) {
        if (context.getGame() instanceof DbTmpGame game) {
            this.sendSelectionTo(sender, this.createSelection(MAX_PLAYERS)
                    .addValues(IntStream.range(game.getMinPlayerNumber(), game.getMaxPlayers() + 1)
                            .mapToObj(String::valueOf).toList()));
        } else if (context.getGame() instanceof DbNonTmpGame game) {
            this.sendSelectionTo(sender, this.createSelection(MAX_PLAYERS)
                    .addValues(IntStream.range(2, game.getMaxPlayers() + 1)
                            .mapToObj(String::valueOf).toList()));
        }
    }

    private void checkMaps(Sender sender, Context context) {
        Availability maps = context.getOption(GAME).getInfo().getMapAvailability();
        if (maps == Availability.ALLOWED) {
            this.sendSelectionTo(sender, this.createSelection(MAPS).addValues("yes", "no"));
        } else if (maps == Availability.REQUIRED) {
            context.addOption(MAPS, true);
            this.checkKits(sender, context);
        } else if (maps == Availability.FORBIDDEN) {
            context.addOption(MAPS, false);
            this.checkKits(sender, context);
        }
    }

    private void checkKits(Sender sender, Context context) {
        Availability kits = context.getOption(GAME).getInfo().getKitAvailability();
        if (kits == Availability.ALLOWED) {
            this.sendSelectionTo(sender, this.createSelection(KITS).addValues("yes", "no"));
        } else if (kits == Availability.REQUIRED) {
            context.addOption(KITS, true);
            this.checkTeamsSizes(sender, context);
        } else if (kits == Availability.FORBIDDEN) {
            context.addOption(KITS, false);
            this.checkTeamsSizes(sender, context);
        }
    }

    private void checkTeamsSizes(Sender sender, Context context) {
        if (context.getOption(GAME) instanceof DbTmpGame game) {
            LinkedList<String> sizes = new LinkedList<>();

            List<Integer> teams = new ArrayList<>(game.getTeamSizes());
            teams.sort(Integer::compareTo);
            teams.sort(Comparator.reverseOrder());

            if (teams.size() <= 1) {
                context.addOption(TEAM_MERGE, false);
                this.checkOldPvP(sender, context);
                return;
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

            this.sendSelectionTo(sender, this.createSelection(TEAM_SIZE)
                    .addValues(sizes));
        } else if (context.getOption(GAME) instanceof DbNonTmpGame) {
            this.checkOldPvP(sender, context);
        }
    }

    private void checkOldPvP(Sender sender, Context context) {
        Type.Availability oldPvP = context.getOption(GAME).getInfo().getOldPvPAvailability();
        if (oldPvP == Availability.ALLOWED) {
            this.sendSelectionTo(sender, this.createSelection(OLD_PVP)
                    .addValues("yes", "no"));
        } else if (oldPvP == Availability.REQUIRED) {
            context.addOption(OLD_PVP, true);
            this.startGame(sender, context);
        } else if (oldPvP == Availability.FORBIDDEN) {
            context.addOption(OLD_PVP, false);
            this.startGame(sender, context);
        }
    }

    private void startGame(Sender sender, Context context) {
        DbTmpGame game = (DbTmpGame) context.getOption(GAME);
        String gameName = game.getInfo().getName();
        Boolean mapsEnabled = context.getOption(MAPS);
        Boolean kitsEnabled = context.getOption(KITS);
        Integer maxServerPlayers = context.getOption(MAX_PLAYERS);
        Integer teamSize = context.getOption(TEAM_SIZE);
        Integer teamAmount = teamSize != null ?
                (int) Math.ceil(maxServerPlayers / ((double) teamSize)) : null;
        Boolean teamMerging = context.getOption(TEAM_MERGE);
        Boolean oldPvP = context.getOption(OLD_PVP);

        Network.runTaskAsync(() -> {
            sender.sendPluginMessage(text("Creating server...", PERSONAL));
            int loungePort = Network.nextEmptyPort();
            NetworkServer loungeNetworkServer = new NetworkServer((loungePort % 1000) +
                    Type.Server.LOUNGE.getShortName() + Network.TMP_SERVER_SUFFIX, loungePort,
                    Type.Server.LOUNGE,
                    Network.getVelocitySecret());

            Tuple<ServerCreationResult, Optional<Server>> loungeResult = Network.createTmpServer(
                    loungeNetworkServer, true, false);

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
                    tempGamePort, Type.Server.TEMP_GAME, Network.getVelocitySecret()).setTask(
                    gameName);

            if (game.getInfo().getPlayerTrackingRange() != null) {
                gameNetworkServer.setPlayerTrackingRange(game.getInfo().getPlayerTrackingRange());
            }

            if (game.getInfo().getMaxHealth() != null) {
                gameNetworkServer.setMaxHealth(game.getInfo().getMaxHealth());
            }

            Tuple<ServerCreationResult, Optional<Server>> tempServerResult = Network.createTmpServer(
                    gameNetworkServer, mapsEnabled, false);
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

            sender.sendPluginTDMessage("§sStarted game §v" + gameName);
            sender.sendPluginTDMessage("§sGame server: §v" + tmpGameServer.getName());
            sender.sendPluginTDMessage("§sLounge server: §v" + loungeServer.getName());
            sender.sendPluginTDMessage("§sMax players: §v" + "" + maxServerPlayers);
            sender.sendPluginTDMessage("§sMaps: §v" + mapsEnabled);
            sender.sendPluginTDMessage("§sKits: §v" + kitsEnabled);
            sender.sendPluginTDMessage("§sTeam amount: §v" + teamAmount);
            sender.sendPluginTDMessage("§sTeam merging: §v" + teamMerging);
            sender.sendPluginTDMessage("§sTeam size: §v" + "" + teamSize);
            sender.sendPluginTDMessage("§sOld PvP: §v" + oldPvP);

            Network.getBukkitCmdHandler().handleServerCmd(sender, loungeServer);

        });
    }

    @Override
    public Collection<IncCommandOption<?>> getOptions() {
        return OPTIONS;
    }

    @Override
    public String getCommand() {
        return "game";
    }

    public static class Context extends IncCommandContext {

        public DbGame getGame() {
            return super.getOption(GAME);
        }
    }

    private static final IncCommandOption<DbGame> GAME = new IncCommandOption<>("game", "Game") {
        @Override
        public DbGame parseValue(String key) {
            return Database.getGames().getGame(key);
        }
    };
    private static final IncCommandOption<Integer> MAX_PLAYERS = new IncCommandOption.Int(
            "max_players", "Max Players");
    private static final IncCommandOption<Boolean> MAPS = new IncCommandOption.Bool("maps", "Maps");
    private static final IncCommandOption<Boolean> KITS = new IncCommandOption.Bool("kits", "Kits");
    private static final IncCommandOption<Integer> TEAM_SIZE = new IncCommandOption<>("team_size",
            "Team Size") {
        @Override
        public Integer parseValue(String key) {
            if (key.equalsIgnoreCase("solo")) {
                return 0;
            }
            return Integer.valueOf(key);
        }
    };
    private static final IncCommandOption<Boolean> TEAM_MERGE = new IncCommandOption.Bool(
            "team_merge", "Team Merge");
    private static final IncCommandOption<Boolean> OLD_PVP = new IncCommandOption.Bool("pvp",
            "Old PvP");

    private static final List<IncCommandOption<?>> OPTIONS = List.of(GAME, MAX_PLAYERS, MAPS, KITS,
            TEAM_SIZE, TEAM_MERGE, OLD_PVP);
}
