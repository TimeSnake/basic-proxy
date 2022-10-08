/*
 * basic-proxy.main
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

package de.timesnake.basic.proxy.core.channel;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.library.extension.util.chat.Plugin;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class ChannelCmdHandler implements ChannelListener {

    @ChannelHandler(type = {ListenerType.SERVER_COMMAND, ListenerType.SERVER_RESTART, ListenerType.SERVER_DESTROY,
            ListenerType.SERVER_KILL_DESTROY})
    public void onServerMessage(ChannelServerMessage<?> msg) {
        MessageType<?> type = msg.getMessageType();
        String serverName = msg.getName();

        if (type.equals(MessageType.Server.COMMAND)) {
            if (msg.getValue() != null) {
                Network.runCommand((String) msg.getValue());
            }
        } else if (type.equals(MessageType.Server.RESTART)) {
            if (msg.getValue() != null) {
                Integer delaySec = (Integer) msg.getValue();
                Server server = Network.getServer(serverName);
                String name = server.getName();
                Network.printText(Plugin.SYSTEM, "Scheduled restart of server " + name + " (" + delaySec + "s)");
                int maxPlayers = server.getMaxPlayers();
                Network.runTaskLater(() -> {
                    try {
                        Thread.sleep(1000L * delaySec);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Network.runCommand("start server " + name + " " + maxPlayers);
                }, Duration.ofSeconds(delaySec));
            }
        } else if (type.equals(MessageType.Server.DESTROY)) {
            Integer delaySec = ((Integer) msg.getValue());
            if (delaySec == null) {
                delaySec = 0;
            }

            Network.runTaskLater(() -> {
                if (!Network.deleteServer(serverName, true)) {
                    Network.printWarning(Plugin.NETWORK, "Server " + serverName + " could not be deleted");
                }
            }, Duration.ofSeconds(delaySec));
        } else if (type.equals(MessageType.Server.KILL_DESTROY)) {
            Long pid = ((Long) msg.getValue());
            if (pid == null) {
                return;
            }

            Network.runTaskAsync(() -> {
                try {
                    if (!Network.killAndDeleteServer(serverName, pid).get()) {
                        Network.printWarning(Plugin.NETWORK, "Server " + serverName + " could not be killed and deleted");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    Network.printWarning(Plugin.NETWORK, "Server " + serverName + " could not be killed and deleted");
                }
            });

        }
    }
}
