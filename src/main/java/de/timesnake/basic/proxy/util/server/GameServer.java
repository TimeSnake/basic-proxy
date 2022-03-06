package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbGameServer;

public class GameServer extends TaskServer {

    public GameServer(DbGameServer database, String folderPath) {
        super(database, folderPath);
    }

}
