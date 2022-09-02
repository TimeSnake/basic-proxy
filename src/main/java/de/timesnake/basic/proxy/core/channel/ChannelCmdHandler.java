package de.timesnake.basic.proxy.core.channel;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.library.basic.util.chat.Plugin;

public class ChannelCmdHandler implements ChannelListener {

    @ChannelHandler(type = {ListenerType.SERVER_COMMAND, ListenerType.SERVER_RESTART})
    public void onServerMessage(ChannelServerMessage<?> msg) {
        if (msg.getMessageType().equals(MessageType.Server.COMMAND)) {
            if (msg.getValue() != null) {
                Network.runCommand((String) msg.getValue());
            }
        } else if (msg.getMessageType().equals(MessageType.Server.RESTART)) {
            if (msg.getValue() != null) {
                Integer delaySec = (Integer) msg.getValue();
                Server server = Network.getServer(msg.getName());
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
