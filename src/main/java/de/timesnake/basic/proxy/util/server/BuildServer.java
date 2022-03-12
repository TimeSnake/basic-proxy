package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbBuildServer;

public class BuildServer extends TaskServer {

    public BuildServer(DbBuildServer database, String folderPath) {
        super(database, folderPath);
    }

    @Override
    public String getServerTask() {
        return super.getType().getDatabaseValue();
    }
}
