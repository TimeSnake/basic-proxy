package de.timesnake.basic.proxy.core.channel;

import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.core.NetworkChannel;
import de.timesnake.channel.proxy.channel.Channel;
import de.timesnake.channel.proxy.listener.ChannelTimeOutListener;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.basic.util.chat.Plugin;

import java.util.concurrent.TimeUnit;

public class ChannelPingPong implements ChannelTimeOutListener {

    private ScheduledTask task;

    public void startPingPong() {
        if (task != null) {
            task.cancel();
        }
        ((Channel) NetworkChannel.getChannel()).getPingPong().ping(Network.getNotOfflineServerPorts());
        task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
            ((Channel) NetworkChannel.getChannel()).getPingPong().checkPong();
            this.startPingPong();
        }).delay(15, TimeUnit.SECONDS).schedule();
    }

    @Override
    public void onServerTimeOut(Integer port) {
        Network.getServer(port).setStatus(Status.Server.OFFLINE, true);
        Network.printText(Plugin.NETWORK, port + " timed out on channel ping");
    }
}
