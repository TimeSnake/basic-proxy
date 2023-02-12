/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.cmd;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.user.DbUser;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.extension.util.chat.Code;
import de.timesnake.library.extension.util.chat.Plugin;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;
import java.util.List;
import net.kyori.adventure.text.Component;

public class AirModeCmd implements CommandListener<Sender, Argument> {

    private Code perm;

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        if (sender.hasPermission(this.perm)) {
            if (sender.isPlayer(false)) {
                User user = sender.getUser();
                if (user.isAirMode()) {
                    user.setAirMode(false);
                    user.getPlayer()
                            .disconnect(Component.text("Back on the ground. Please rejoin"));
                } else {
                    user.setAirMode(true);
                    user.getPlayer().disconnect(Component.text("Up in the air. Please rejoin"));
                }
            } else {
                if (args.isLengthEquals(1, true)) {
                    if (args.get(0).isPlayerDatabaseName(true)) {
                        if (args.get(0).isPlayerName(false)) {
                            sender.sendPluginMessage(Component.text("Only for offline players",
                                    ExTextColor.WARNING));
                            return;
                        }

                        DbUser user = args.get(0).toDbUser();
                        if (user.isAirMode()) {
                            user.setAirMode(false);
                            sender.sendPluginMessage(Component.text("Disabled air mode for user ",
                                            ExTextColor.PERSONAL)
                                    .append(Component.text(user.getName(), ExTextColor.VALUE)));
                        } else {
                            user.setAirMode(true);
                            sender.sendPluginMessage(Component.text("Enabled air mode for user ",
                                            ExTextColor.PERSONAL)
                                    .append(Component.text(user.getName(), ExTextColor.VALUE)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd,
            Arguments<Argument> args) {
        return null;
    }

    @Override
    public void loadCodes(Plugin plugin) {
        this.perm = plugin.createPermssionCode("air");
    }
}
