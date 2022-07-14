package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbBuildServer;

import java.nio.file.Path;

public class BuildServer extends TaskServer {

    public BuildServer(DbBuildServer database, Path folderPath) {
        super(database, folderPath);
    }

    @Override
    public String getServerTask() {
        return super.getType().getDatabaseValue();
    }
}
