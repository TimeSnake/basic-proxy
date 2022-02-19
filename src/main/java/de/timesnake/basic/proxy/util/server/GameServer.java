package de.timesnake.basic.proxy.util.server;

import de.timesnake.basic.proxy.util.Network;
import de.timesnake.database.util.server.DbGameServer;
import de.timesnake.library.basic.util.Status;

public class GameServer extends TaskServer {

    public GameServer(DbGameServer database, String folderPath) {
        super(database, folderPath);
        this.task = database.getTask();
    }

    @Override
    public void setStatus(Status.Server status, boolean updateDatabase) {
        super.setStatus(status, updateDatabase);
        if (super.status.equals(Status.Server.OFFLINE)) {
            this.task = Network.getServerConfig().getServerTask(this);
            ((DbGameServer) super.database).setTask(this.task);
        }
    }

    @Override
    public void updateStatus() {
        super.status = super.database.getStatus();
        if (super.status.equals(Status.Server.OFFLINE)) {
            this.task = Network.getServerConfig().getServerTask(this);
            ((DbGameServer) super.database).setTask(this.task);
        }
    }

}
