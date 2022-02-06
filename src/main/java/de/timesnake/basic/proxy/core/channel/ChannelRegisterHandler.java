package de.timesnake.basic.proxy.core.channel;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.channel.proxy.listener.ChannelRegisterListener;
import de.timesnake.database.util.Database;
import de.timesnake.database.util.object.Status;

public class ChannelRegisterHandler implements ChannelRegisterListener {

    public ChannelRegisterHandler() {
        Network.getChannel().addRegisterListener(this);
    }

    @Override
    public void onChannelRegisterMessage(int port, boolean isRegistering) {
        if (!isRegistering) {
            Database.getServers().getServer(port).setStatus(Status.Server.OFFLINE);
        }
    }

}