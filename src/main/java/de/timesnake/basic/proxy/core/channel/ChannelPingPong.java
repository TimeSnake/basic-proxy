/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.channel;

import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.core.ServerChannel;
import de.timesnake.channel.proxy.channel.ProxyChannel;
import de.timesnake.channel.proxy.listener.ChannelTimeOutListener;
import de.timesnake.library.basic.util.Loggers;
import de.timesnake.library.basic.util.Status;

import java.util.concurrent.TimeUnit;

public class ChannelPingPong implements ChannelTimeOutListener {

  private ScheduledTask task;

  public void startPingPong() {
    if (task != null) {
      task.cancel();
    }
    ((ProxyChannel) ServerChannel.getInstance()).ping(Network.getNotOfflineServerNames());
    task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
      ((ProxyChannel) ServerChannel.getInstance()).checkServerPong();
      this.startPingPong();
    }).delay(15, TimeUnit.SECONDS).schedule();
  }

  @Override
  public void onServerTimeOut(String name) {
    Network.getServer(name).setStatus(Status.Server.OFFLINE, true);
    Loggers.NETWORK.warning(name + " timed out on channel ping");
  }
}
