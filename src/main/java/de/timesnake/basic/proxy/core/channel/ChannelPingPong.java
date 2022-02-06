package de.timesnake.basic.proxy.core.channel;

import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.main.NetworkChannel;
import de.timesnake.channel.proxy.channel.Channel;
import de.timesnake.channel.proxy.listener.ChannelTimeOutListener;
import de.timesnake.database.util.object.Status;
import de.timesnake.library.basic.util.chat.Plugin;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class ChannelPingPong implements ChannelTimeOutListener {

    private ScheduledTask task;

    public void startPingPong() {
        if (task != null) {
            task.cancel();
        }
        ((Channel) NetworkChannel.getChannel()).getPingPong().ping(Network.getNotOfflineServerPorts());
        task = ProxyServer.getInstance().getScheduler().schedule(BasicProxy.getPlugin(), () -> {
            ((Channel) NetworkChannel.getChannel()).getPingPong().checkPong();
            this.startPingPong();
        }, 15, TimeUnit.SECONDS);
    }

    @Override
    public void onServerTimeOut(Integer port) {
        Network.getServer(port).setStatus(Status.Server.OFFLINE, true);
        Network.printText(Plugin.NETWORK, port + " timed out on channel ping");
    }
}
