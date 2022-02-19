package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.Status;

public class LoungeServer extends TaskServer {

    public LoungeServer(DbLoungeServer database, String folderPath) {
        super(database, folderPath);
    }

    @Override
    public void setStatus(Status.Server status, boolean updateDatabase) {
        super.status = status;
        if (updateDatabase) {
            super.database.setStatus(status);
        }

        if (super.status.equals(Status.Server.OFFLINE)) {
            ((DbLoungeServer) super.database).setTask(null);
        }
    }

    @Override
    public void updateStatus() {
        super.status = super.database.getStatus();
        if (super.status.equals(Status.Server.OFFLINE)) {
            ((DbLoungeServer) super.database).setTask(null);
        }
    }
}
