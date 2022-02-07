package de.timesnake.basic.proxy.core.coins;


import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class CoinsCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (sender.hasPermission("timecoins.settings", 34)) {
            if (args.isLengthEquals(3, true)) {
                if (args.get(0).isPlayerName(true)) {
                    User user = args.get(0).toUser();
                    if (args.get(2).isFloat(true)) {
                        float coins = args.get(2).toFloat();
                        switch (args.get(1).toLowerCase()) {
                            case "add":
                                user.addCoins(coins);
                                sender.sendPluginMessage(ChatColor.PERSONAL + "Added " + ChatColor.VALUE + coins + ChatColor.PERSONAL + " timecoin(s) to " + user.getChatName());
                                break;
                            case "remove":
                                user.removeCoins(coins);
                                sender.sendPluginMessage(ChatColor.PERSONAL + "Removed " + ChatColor.VALUE + coins + ChatColor.PERSONAL + " timecoin(s) from " + user.getChatName());
                                break;
                            case "set":
                                user.setCoins(coins);
                                sender.sendPluginMessage(ChatColor.PERSONAL + "Set balance to " + ChatColor.VALUE + coins + ChatColor.PERSONAL + " timecoin(s) for " + user.getChatName());
                                break;
                            case "reset":
                                user.setCoins(0);
                                sender.sendPluginMessage(ChatColor.PERSONAL + "Set balance to " + ChatColor.VALUE + coins + ChatColor.PERSONAL + " timecoin(s) for " + user.getChatName());
                                break;
                            default:

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
            return List.of("0", "1", "10");
        }
        return null;
    }
}
