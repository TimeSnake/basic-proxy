package de.timesnake.basic.proxy.core.channel;

import de.timesnake.basic.proxy.util.chat.Argument;
import de.timesnake.basic.proxy.util.chat.ChatColor;
import de.timesnake.basic.proxy.util.chat.Sender;
import de.timesnake.channel.channel.ChannelInfo;
import de.timesnake.library.extension.util.cmd.Arguments;
import de.timesnake.library.extension.util.cmd.CommandListener;
import de.timesnake.library.extension.util.cmd.ExCommand;

import java.util.List;

public class ChannelBroadcastCmd implements CommandListener<Sender, Argument> {

    @Override
    public void onCommand(Sender sender, ExCommand<Sender, Argument> cmd, Arguments<Argument> args) {
        if (sender.isConsole(true)) {
            ChannelInfo.setBroadcast(!ChannelInfo.getBroadcast());
            sender.sendPluginMessage(ChatColor.PERSONAL + "Broadcast channel-messages: " + ChannelInfo.getBroadcast());
        }
    }

    @Override
    public List<String> getTabCompletion(ExCommand<Sender, Argument> cmd, Arguments<Argument> arguments) {
        return null;
    }
}
