package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbGameServer;

import java.nio.file.Path;

public class GameServer extends TaskServer {

    public GameServer(DbGameServer database, Path folderPath) {
        super(database, folderPath);
    }

}
