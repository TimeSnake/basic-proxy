package de.timesnake.basic.proxy.core.user;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;

public class AirModeCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (sender.hasPermission("air", 1)) {
            if (sender.isPlayer(false)) {
                User user = sender.getUser();
                if (user.isAirMode()) {
                    user.setAirMode(false);
                    user.getPlayer().disconnect(new TextComponent("Back on the ground. Please rejoin"));
                } else {
                    user.setAirMode(true);
                    user.getPlayer().disconnect(new TextComponent("Up in the air. Please rejoin"));
                }
            } else {
                if (args.isLengthEquals(1, true)) {
                    if (args.get(0).isPlayerDatabaseName(true)) {
                        if (args.get(0).isPlayerName(false)) {
                            sender.sendPluginMessage(ChatColor.WARNING + "Only for offline players");
                            return;
                        }

                        DbUser user = args.get(0).toDbUser();
                        if (user.isAirMode()) {
                            user.setAirMode(false);
                            sender.sendPluginMessage(ChatColor.PERSONAL + "Disabled air mode for user " + user.getName());
                        } else {
                            user.setAirMode(true);
                            sender.sendPluginMessage(ChatColor.PERSONAL + "Enabled air mode for user " + user.getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return null;
    }
}
