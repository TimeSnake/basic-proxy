/*
 * workspace.basic-proxy.main
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

import com.velocitypowered.api.scheduler.ScheduledTask;
import de.timesnake.basic.proxy.core.main.BasicProxy;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.core.Channel;
import de.timesnake.channel.proxy.channel.ProxyChannel;
import de.timesnake.channel.proxy.listener.ChannelTimeOutListener;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.extension.util.chat.Plugin;

import java.util.concurrent.TimeUnit;

public class ChannelPingPong implements ChannelTimeOutListener {

    private ScheduledTask task;

    public void startPingPong() {
        if (task != null) {
            task.cancel();
        }
        ((ProxyChannel) Channel.getInstance()).ping(Network.getNotOfflineServerNames());
        task = BasicProxy.getServer().getScheduler().buildTask(BasicProxy.getPlugin(), () -> {
            ((ProxyChannel) Channel.getInstance()).checkPong();
            this.startPingPong();
        }).delay(15, TimeUnit.SECONDS).schedule();
    }

    @Override
    public void onServerTimeOut(String name) {
        Network.getServer(name).setStatus(Status.Server.OFFLINE, true);
        Network.printText(Plugin.NETWORK, name + " timed out on channel ping");
    }
}
