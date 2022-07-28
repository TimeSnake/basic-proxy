package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbNonTmpGameServer;

import java.nio.file.Path;

public class GameServer extends TaskServer {

    public GameServer(DbNonTmpGameServer database, Path folderPath) {
        super(database, folderPath);
    }

}
