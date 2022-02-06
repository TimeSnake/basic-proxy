package de.timesnake.basic.proxy.core.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.library.basic.util.cmd.Arguments;
import de.timesnake.library.basic.util.cmd.CommandListener;
import de.timesnake.library.basic.util.cmd.ExCommand;

import java.util.List;

public class ServiceCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        Network.getBukkitCmdHandler().handleServiceCommand(sender, args);
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        return null;
    }
}
