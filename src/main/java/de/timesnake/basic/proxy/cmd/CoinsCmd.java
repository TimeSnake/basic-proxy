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

package de.timesnake.basic.proxy.cmd;


import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.basic.util.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

public class CoinsCmd implements CommandListener<Sender, Argument> {

    private Code.Permission perm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (sender.hasPermission(this.perm)) {
            if (args.isLengthEquals(3, true)) {
                if (args.get(0).isPlayerName(true)) {
                    User user = args.get(0).toUser();
                    if (args.get(2).isFloat(true)) {
                        float coins = args.get(2).toFloat();
                        switch (args.get(1).toLowerCase()) {
                            case "add" -> {
                                user.addCoins(coins);
                                sender.sendPluginMessage(Component.text("Added ")
                                        .append(Component.text(coins, ExTextColor.VALUE))
                                        .append(Component.text(" timecoin(s) to ", ExTextColor.PERSONAL))
                                        .append(user.getChatNameComponent()));
                            }
                            case "remove" -> {
                                user.removeCoins(coins);
                                sender.sendPluginMessage(Component.text("Removed ")
                                        .append(Component.text(coins, ExTextColor.VALUE))
                                        .append(Component.text(" timecoin(s) from ", ExTextColor.PERSONAL))
                                        .append(user.getChatNameComponent()));
                            }
                            case "set" -> {
                                user.setCoins(coins);
                                sender.sendPluginMessage(Component.text("Set balance to ")
                                        .append(Component.text(coins, ExTextColor.VALUE))
                                        .append(Component.text(" timecoin(s) for ", ExTextColor.PERSONAL))
                                        .append(user.getChatNameComponent()));
                            }
                            case "reset" -> {
                                user.setCoins(0);
                                sender.sendPluginMessage(Component.text("Set balance to ")
                                        .append(Component.text(coins, ExTextColor.VALUE))
                                        .append(Component.text(" timecoin(s) for ", ExTextColor.PERSONAL))
                                        .append(user.getChatNameComponent()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (args.getLength() == 1) {
            return Network.getCommandHandler().getPlayerNames();
        }
        if (args.getLength() == 2) {
            return List.of("add", "remove", "set", "reset");
        }
        if (args.getLength() == 3) {
            return List.of("0", "1", "10", "100");
        }
        return null;
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.perm = plugin.createPermssionCode("cns", "timecoins.settings");
    }
}
