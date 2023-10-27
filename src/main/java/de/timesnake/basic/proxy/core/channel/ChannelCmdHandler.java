/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.channel;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.server.Server;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.library.basic.util.Loggers;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class ChannelCmdHandler implements ChannelListener {

  @ChannelHandler(type = {ListenerType.SERVER_COMMAND, ListenerType.SERVER_RESTART,
      ListenerType.SERVER_DESTROY,
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
        Loggers.NETWORK.info("Scheduled restart of server " + name + " (" + delaySec + "s)");
        int maxPlayers = server.getMaxPlayers();
        Network.runTaskLater(() -> {
          try {
            Thread.sleep(1000L * delaySec);
          } catch (InterruptedException ignored) {
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
          Loggers.NETWORK.warning("Failed to delete server '" + serverName + "'");
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
            Loggers.NETWORK.warning("Failed to kill and delete server '" + serverName + "'");
          }
        } catch (InterruptedException | ExecutionException e) {
          Loggers.NETWORK.warning("Exception while killing and deleting server '" + serverName + "': " + e.getMessage());
        }
      });

    }
  }
}
