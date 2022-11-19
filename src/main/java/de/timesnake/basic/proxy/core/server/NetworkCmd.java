/*
 * workspace.basic-proxy.main
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
import de.timesnake.database.util.Database;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.game.DbNonTmpGame;
import de.timesnake.database.util.object.Type;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import de.timesnake.library.network.ServerInitResult;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.List;

import static de.timesnake.library.basic.util.chat.ExTextColor.PERSONAL;
import static de.timesnake.library.basic.util.chat.ExTextColor.WARNING;

public class NetworkCmd implements CommandListener<Sender, Argument> {

    private Code.Help serverAlreadyExists;
    private Code.Permission createOwnPerm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (!args.isLengthHigherEquals(1, true)) {
            return;
        }

        switch (args.getString(0).toLowerCase()) {
            case "create_own_game" -> this.handleCreateOwnGameCmd(sender, args);
            case "create_public_game" -> this.handleCreatePublicGameCmd(sender, args);
        }
    }

    private void handleCreatePublicGameCmd(Sender sender, Arguments<Argument> args) {
        if (!sender.hasPermission("network.create.public_game")) {
            return;
        }

        if (!args.isLengthHigherEquals(3, true)) {
            return;
        }

        DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

        if (!(game instanceof DbNonTmpGame nonTmpGame)) {
            sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
            return;
        }

        Collection<String> serverNames = Network.getNetworkUtils().getPublicPlayerServerNames(Type.Server.GAME,
                nonTmpGame.getName());

        String serverName = args.get(2).toLowerCase();

        if (serverNames.contains(serverName)) {
            sender.sendMessageAlreadyExist(serverName, this.serverAlreadyExists, "server");
            return;
        }

        ServerInitResult result = Network.createPublicPlayerServer(Type.Server.GAME, ((DbNonTmpGame) game).getName(),
                serverName);

        if (!result.isSuccessful()) {
            sender.sendPluginMessage(Component.text("Error while creating server (" +
                    ((ServerInitResult.Fail) result).getReason() + ")", WARNING));
            return;
        }

        sender.sendPluginMessage(Component.text("Created server ", PERSONAL)
                .append(Component.text(serverName, ExTextColor.VALUE))
                .append(Component.text(" (" + serverName + ")", ExTextColor.QUICK_INFO)));
    }

    private void handleCreateOwnGameCmd(Sender sender, Arguments<Argument> args) {
        if (!sender.hasPermission(this.createOwnPerm)) {
            return;
        }

        if (!args.isLengthEquals(4, true)) {
            return;
        }

        DbGame game = Database.getGames().getGame(args.getString(1).toLowerCase());

        if (!(game instanceof DbNonTmpGame nonTmpGame)) {
            sender.sendPluginMessage(Component.text("Unsupported game type", WARNING));
            return;
        }

        if (!nonTmpGame.isOwnable()) {
            sender.sendPluginMessage(Component.text("Servers of this game can not have an owner", WARNING));
            return;
        }

        Argument playerArg = args.get(3);

        if (!playerArg.isPlayerDatabaseName(true)) {
            return;
        }

        DbUser user = playerArg.toDbUser();

        Collection<String> serverNames = Network.getNetworkUtils().getOwnerServerNames(user.getUniqueId(), Type.Server.GAME,
                ((DbNonTmpGame) game).getName());

        String serverName = args.get(2).toLowerCase();

        if (serverNames.contains(user.getUniqueId().hashCode() + serverName)) {
            sender.sendMessageAlreadyExist(serverName, this.serverAlreadyExists, "server");
            return;
        }

        ServerInitResult result = Network.createPlayerServer(user.getUniqueId(), Type.Server.GAME, ((DbNonTmpGame) game).getName(),
                user.getUniqueId().hashCode() + serverName);

        if (!result.isSuccessful()) {
            sender.sendPluginMessage(Component.text("Error while creating server (" +
                    ((ServerInitResult.Fail) result).getReason() + ")", WARNING));
            return;
        }

        sender.sendPluginMessage(Component.text("Created server ", PERSONAL)
                .append(Component.text(serverName, ExTextColor.VALUE))
                .append(Component.text(" (" + user.getUniqueId().hashCode() + serverName + ")", ExTextColor.QUICK_INFO)));
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.length() == 1) {
            return List.of("create_own_game");
        } else if (args.length() == 2) {
            if (args.getString(0).equalsIgnoreCase("create_own_game")) {
                return Network.getCommandHandler().getGameNames();
            }
        } else if (args.length() == 3) {
            if (args.getString(0).equalsIgnoreCase("create_own_game")) {
                return List.of("<name>");
            }
        } else if (args.length() == 4) {
            if (args.getString(0).equalsIgnoreCase("create_own_game")) {
                return Network.getCommandHandler().getPlayerNames();
            }
        }

        return List.of();
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.serverAlreadyExists = plugin.createHelpCode("prx", "Server name already exists");
        this.createOwnPerm = plugin.createPermssionCode("prx", "network.create.own_game");
    }
}
