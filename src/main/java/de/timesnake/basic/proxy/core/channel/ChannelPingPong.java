/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.core.channel;

import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.proxy.main.ChannelTimeOutListener;
import de.timesnake.channel.proxy.main.ProxyChannel;
import de.timesnake.library.basic.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class ChannelPingPong implements ChannelTimeOutListener {

  private final Logger logger = LogManager.getLogger("network.ping");

  private ScheduledTask task;

  public void startPingPong() {
    if (task != null) {
      task.cancel();
    }
    task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
      ProxyChannel.getInstance().checkServerPong();
      ProxyChannel.getInstance().ping(Network.getNotOfflineServerNames());
    }).repeat(15, TimeUnit.SECONDS).schedule();
  }

  @Override
  public void onServerTimeOut(String name) {
    Network.getServer(name).setStatus(Status.Server.OFFLINE, true);
    this.logger.warn("{} timed out on channel ping", name);
  }
}
