package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbPvPServer;

import java.nio.file.Path;

public abstract class PvPServer extends TaskServer {

    private boolean oldPvP;

    protected PvPServer(DbPvPServer database, Path folderPath) {
        super(database, folderPath);
        this.oldPvP = database.isOldPvP();
    }

    public boolean isOldPvP() {
        return this.oldPvP;
    }

    public void setPvP(boolean oldPvP) {
        if (oldPvP != this.oldPvP) {
            ((DbPvPServer) this.getDatabase()).setPvP(oldPvP);
        }
        this.oldPvP = oldPvP;
    }

}
