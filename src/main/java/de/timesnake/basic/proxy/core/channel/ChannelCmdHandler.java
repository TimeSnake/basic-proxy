package de.timesnake.basic.proxy.core.channel;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.channel.api.message.ChannelServerMessage;
import de.timesnake.channel.listener.ChannelServerListener;
import de.timesnake.library.basic.util.chat.Plugin;

public class ChannelCmdHandler implements ChannelServerListener {

    @Override
    public void onServerMessage(ChannelServerMessage msg) {
        if (msg.getType().equals(ChannelServerMessage.MessageType.COMMAND)) {
            if (msg.getValue() != null) {
                Network.runCommand(msg.getValue());
            }
        } else if (msg.getType().equals(ChannelServerMessage.MessageType.RESTART)) {
            if (msg.getValue() != null) {
                int delaySec = Integer.parseInt(msg.getValue());
                Server server = Network.getServer(msg.getPort());
                String name = server.getName();
                Network.printText(Plugin.SYSTEM, "Scheduled restart of server " + name + " (" + delaySec + "s)");
                int maxPlayers = server.getMaxPlayers();
                new Thread(() -> {
                    try {
                        Thread.sleep(1000L * delaySec);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Network.runCommand("start server " + name + " " + maxPlayers);
                }).start();
            }
        }
    }
}
